import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { TokenStorageService } from '../services/token-storage.service';
import { AuthService } from '../services/auth.service';

interface MenuItem {
  id: string; name: string; category: string; description: string;
  price: number; stock: number; imageUrl?: string;
}
interface CartItem { item: MenuItem; quantity: number; }
interface OrderItem { foodName: string; quantity: number; unitPrice: number; lineTotal: number; }
interface CafeteriaOrder {
  id: number; status: string; total: number; createdAt: string; items: OrderItem[];
  startedAt?: string; readyAt?: string; completedAt?: string; cancelledAt?: string;
}
interface DailyReport { date: string; revenue: number; completedOrders: number; placedOrders: number; }
interface PopularDish { foodId: string; name: string; quantity: number; revenue: number; }
interface LowStock { id: string; name: string; category: string; stock: number; }
interface Dashboard {
  range: string; revenue: number; completedOrders: number; totalOrders: number;
  lowStockCount: number; averagePrepMinutes: number; statusCounts: {[key:string]:number};
  daily: DailyReport[]; popularDishes: PopularDish[]; lowStock: LowStock[];
}
interface InventoryMovement {
  id: number; foodId: string; foodName: string; actorId?: number; orderId?: number;
  reason: string; delta: number; quantityAfter: number; createdAt: string;
}
interface UserSession { accessToken: string; username: string; fullname: string; roles: string[]; }

@Component({
  selector: 'app-homepage',
  templateUrl: './homepage.component.html',
  styleUrls: ['./homepage.component.css']
})
export class HomepageComponent implements OnInit, OnDestroy {
  menu: MenuItem[] = [];
  cart: CartItem[] = [];
  orders: CafeteriaOrder[] = [];
  dashboard: Dashboard | null = null;
  stockHistory: InventoryMovement[] = [];
  reportDays = 7;
  orderFilter = 'ACTIVE';
  inventoryFilter = 'All';
  lowStockThreshold = 5;
  loadingMenu = true;
  loadingOrders = false;
  loadingDashboard = false;
  loadingHistory = false;
  lastOrdersUpdated: Date | null = null;
  checkoutConfirmation: number | null = null;
  private refreshTimer: any = null;
  search = '';
  category = 'All';
  credentials = { username: '', password: '' };
  registration = { username: '', email: '', password: '', fullname: '', orgname: '', empno: 0, mobileno: 0 };
  newFood = { name: '', category: 'Vegetarian', description: '', price: 99, stock: 10 };
  editingStock: {[id: string]: number} = {};
  user: UserSession | null = null;
  error = '';
  success = '';
  busy = false;
  registerOpen = false;
  adminEditorOpen = false;
  editingFoodId: string | null = null;
  activeTab = 'menu';
  private checkoutKey: string | null = null;
  private cartRestoredFor = '';

  constructor(private http: HttpClient, private auth: AuthService, private storage: TokenStorageService) {}

  ngOnInit(): void {
    this.restore();
    this.loadMenu();
    if (this.user) {
      this.loadOrders();
      if (this.isAdmin) { this.loadDashboard(); }
    }
    // Poll only while a relevant screen is visible. No WebSocket infrastructure required.
    this.refreshTimer = setInterval(() => {
      if (!this.user || this.busy) { return; }
      if (this.activeTab === 'orders') { this.loadOrders(true); }
      if (this.activeTab === 'dashboard' && this.isAdmin) { this.loadDashboard(true); }
    }, 15000);
  }
  ngOnDestroy(): void {
    if (this.refreshTimer) { clearInterval(this.refreshTimer); }
  }

  get isAdmin(): boolean { return !!this.user && this.user.roles.indexOf('ROLE_ADMIN') >= 0; }
  get isStaff(): boolean { return !!this.user && (this.isAdmin || this.user.roles.indexOf('ROLE_MODERATOR') >= 0); }
  get categories(): string[] { return ['All'].concat(Array.from(new Set(this.menu.map(f => f.category)))); }
  get filteredMenu(): MenuItem[] {
    const q = this.search.toLowerCase().trim();
    return this.menu.filter(f => (this.category === 'All' || f.category === this.category) &&
      (!q || (f.name + ' ' + f.description + ' ' + f.category).toLowerCase().includes(q)));
  }
  get count(): number { return this.cart.reduce((sum, line) => sum + line.quantity, 0); }
  get total(): number { return this.cart.reduce((sum, line) => sum + line.item.price * line.quantity, 0); }
  get unavailable(): boolean { return this.cart.some(line => line.quantity > line.item.stock); }
  get visibleOrders(): CafeteriaOrder[] {
    return this.orders.filter(order => this.orderFilter === 'ALL' ||
      (this.orderFilter === 'ACTIVE'
        ? ['PLACED','PREPARING','READY'].indexOf(order.status) >= 0
        : order.status === this.orderFilter));
  }
  get filteredInventory(): MenuItem[] {
    return this.menu.filter(item => this.inventoryFilter === 'All' ||
      (this.inventoryFilter === 'Low stock' ? item.stock > 0 && item.stock <= this.lowStockThreshold
        : this.inventoryFilter === 'Sold out' ? item.stock === 0 : item.category === this.inventoryFilter));
  }
  get maxDailyRevenue(): number {
    return Math.max(1, ...((this.dashboard && this.dashboard.daily || []).map(d => d.revenue)));
  }
  revenueBar(revenue: number): number {
    return Math.max(2, Math.round(revenue / this.maxDailyRevenue * 100));
  }
  get activeOrderCount(): number {
    return this.orders.filter(o => ['PLACED','PREPARING','READY'].indexOf(o.status) >= 0).length;
  }
  orderSteps(order: CafeteriaOrder): string[] {
    return order.status === 'CANCELLED'
      ? ['PLACED','CANCELLED'] : ['PLACED','PREPARING','READY','COMPLETED'];
  }
  stepReached(order: CafeteriaOrder, step: string): boolean {
    if (order.status === 'CANCELLED') { return step === 'PLACED' || step === 'CANCELLED'; }
    return this.orderSteps(order).indexOf(step) <= this.orderSteps(order).indexOf(order.status);
  }
  stepLabel(step: string): string {
    return ({PLACED:'Placed',PREPARING:'Preparing',READY:'Ready for pickup',
      COMPLETED:'Completed',CANCELLED:'Cancelled'} as {[key:string]:string})[step] || step;
  }
  prepElapsed(order: CafeteriaOrder): string {
    if (!order.startedAt) { return 'Not started'; }
    const end = order.readyAt ? new Date(order.readyAt).getTime() : Date.now();
    const minutes = Math.max(0, Math.round((end - new Date(order.startedAt).getTime()) / 60000));
    return minutes + ' min' + (order.readyAt ? ' to ready' : ' preparing');
  }
  showDashboard(): void {
    this.activeTab='dashboard';
    this.loadDashboard();
  }
  showInventory(): void {
    this.activeTab='manage';
    this.loadHistory();
    this.loadMenu();
  }
  showOrders(): void {
    this.orderFilter=this.isStaff ? 'ACTIVE' : 'ALL';
    this.activeTab='orders';
    this.loadOrders();
  }
  private cartKey(): string {
    return this.user ? 'cafeteria-cart-v2-' + this.user.username : '';
  }
  private persistCart(): void {
    if (!this.user || this.isStaff) { return; }
    try {
      localStorage.setItem(this.cartKey(),JSON.stringify({
        lines:this.cart.map(line => ({id:line.item.id,quantity:line.quantity})),
        key:this.checkoutKey
      }));
    } catch (_) { /* Private browsing may disable storage; in-memory cart still works. */ }
  }
  private restoreCart(): void {
    if (!this.user || this.isStaff || !this.menu.length) { return; }
    if (this.cartRestoredFor === this.user.username) { return; }
    this.cartRestoredFor = this.user.username;
    try {
      const data=JSON.parse(localStorage.getItem(this.cartKey()) || 'null');
      if (!data || !Array.isArray(data.lines)) { return; }
      this.cart=data.lines.map((line: any) => ({
        item:this.menu.find(f => f.id === line.id),
        quantity:Math.min(50,Math.floor(Number(line.quantity)))
      })).filter((line: any) => !!line.item && line.quantity > 0);
      this.checkoutKey=typeof data.key === 'string' ? data.key : null;
    } catch (_) { this.cart=[]; this.checkoutKey=null; }
  }
  private clearSavedCart(): void {
    if (!this.user) { return; }
    try { localStorage.removeItem(this.cartKey()); } catch (_) {}
  }
  loadDashboard(silent = false): void {
    if (!this.isAdmin) { return; }
    if (!silent) { this.loadingDashboard=true; }
    this.http.get<Dashboard>(environment.apiUrl + '/admin/overview?days=' + this.reportDays +
      '&lowStockThreshold=' + this.lowStockThreshold, {headers:this.headers()})
      .subscribe(data => { this.dashboard=data; this.loadingDashboard=false; },
        e => { this.loadingDashboard=false; if (!silent) { this.fail(e); } });
  }
  loadHistory(): void {
    if (!this.isAdmin) { return; }
    this.loadingHistory=true;
    this.http.get<InventoryMovement[]>(environment.apiUrl + '/admin/inventory/history',
      {headers:this.headers()}).subscribe(data => {
        this.stockHistory=data; this.loadingHistory=false;
      }, e => { this.loadingHistory=false; this.fail(e); });
  }


  private headers(): HttpHeaders {
    return new HttpHeaders({ Authorization: 'Bearer ' + this.storage.getToken() });
  }
  private fail(err: any): void {
    this.error = err && err.error && (err.error.message || err.error.error) || 'Unable to complete request. Please try again.';
    this.busy = false;
  }
  private restore(): void {
    const saved = this.storage.getUser();
    this.user = saved && saved.accessToken && saved.roles ? saved : null;
  }
  loadMenu(): void {
    this.loadingMenu=true;
    this.http.get<MenuItem[]>(environment.apiUrl + '/menu').subscribe(
      data => {
        this.menu=data;
        this.loadingMenu=false;
        data.forEach(item => this.editingStock[item.id] = item.stock);
        this.restoreCart();
        this.cart=this.cart.map(line => ({
          item:data.find(f => f.id === line.item.id) || line.item,quantity:line.quantity
        })).filter(line => data.some(f => f.id === line.item.id));
        this.persistCart();
      }, e => { this.loadingMenu=false; this.fail(e); });
  }
  login(): void {
    this.error = ''; this.success = ''; this.busy = true;
    this.auth.login(this.credentials).subscribe(data => {
      this.storage.saveToken(data.accessToken);
      this.storage.saveUser(data);
      this.user = data;
      this.credentials.password = '';
      this.busy = false;
      this.cartRestoredFor='';
      this.cart=[];
      this.checkoutKey=null;
      this.loadMenu();
      this.loadOrders();
      if(this.isAdmin){this.loadDashboard();}
    }, e => this.fail(e));
  }
  register(): void {
    this.error = ''; this.busy = true;
    this.auth.register(this.registration).subscribe(() => {
      this.registerOpen = false; this.busy = false;
      this.success = 'Account created. Sign in to start ordering.';
    }, e => this.fail(e));
  }
  logout(): void {
    this.storage.signOut(); this.user = null; this.orders = []; this.cart = [];
    this.dashboard=null; this.stockHistory=[]; this.cartRestoredFor='';
    this.checkoutKey = null; this.activeTab = 'menu'; this.success = 'Signed out.';
  }
  add(item: MenuItem): void {
    if (!this.user || this.isStaff || item.stock <= 0) { return; }
    const current = this.cart.find(line => line.item.id === item.id);
    if (current) {
      if (current.quantity < item.stock) { current.quantity++; }
    } else { this.cart.push({ item, quantity: 1 }); }
    this.checkoutKey = null;
    this.persistCart();
    this.success = '';
  }
  changeQty(line: CartItem, difference: number): void {
    line.quantity += difference;
    if (line.quantity <= 0) { this.cart = this.cart.filter(x => x !== line); }
    else { line.quantity = Math.min(line.quantity, line.item.stock); }
    this.checkoutKey = null;
    this.persistCart();
  }
  checkout(): void {
    if (!this.user || !this.cart.length || this.unavailable || this.busy) { return; }
    this.busy = true; this.error = ''; this.success = '';
    if (!this.checkoutKey) { this.checkoutKey = 'order-' + Date.now() + '-' + Math.random().toString(36).slice(2); }
    this.persistCart();
    const headers = this.headers().set('Idempotency-Key', this.checkoutKey);
    const items = this.cart.map(line => ({ foodId: line.item.id, quantity: line.quantity }));
    this.http.post<CafeteriaOrder>(environment.apiUrl + '/orders', {items}, {headers}).subscribe(order => {
      this.busy=false; this.cart=[]; this.checkoutKey=null;
      this.clearSavedCart();
      this.checkoutConfirmation=order.id;
      this.success='Order #' + order.id + ' placed successfully.';
      this.activeTab='orders'; this.orderFilter='ALL'; this.loadOrders(); this.loadMenu();
    }, e => {
      this.fail(e);
      // Preserve the idempotency key after an ambiguous transport failure.
      // Retry the exact cart or check order history before editing quantities.
    });
  }
  loadOrders(silent = false): void {
    if (!this.user) { return; }
    if (!silent) { this.loadingOrders=true; }
    const path = this.isStaff ? '/orders' : '/orders/mine';
    this.http.get<CafeteriaOrder[]>(environment.apiUrl + path, {headers: this.headers()})
      .subscribe(data => {
        this.orders=data; this.loadingOrders=false; this.lastOrdersUpdated=new Date();
      }, e => { this.loadingOrders=false; if(!silent){this.fail(e);} });
  }
  nextStatus(status: string): string {
    return ({PLACED:'PREPARING', PREPARING:'READY', READY:'COMPLETED'} as {[key:string]:string})[status] || '';
  }
  advance(order: CafeteriaOrder): void {
    const next = this.nextStatus(order.status);
    if (next) { this.changeOrderStatus(order, next); }
  }
  cancelOrder(order: CafeteriaOrder): void {
    if (!this.isStaff || (order.status !== 'PLACED' && order.status !== 'PREPARING')) { return; }
    if (confirm('Cancel order #' + order.id + ' and return its items to stock?')) {
      this.changeOrderStatus(order, 'CANCELLED');
    }
  }
  private changeOrderStatus(order: CafeteriaOrder, status: string): void {
    if (this.busy) { return; }
    this.busy = true; this.error = ''; this.success = '';
    this.http.patch<CafeteriaOrder>(environment.apiUrl + '/orders/' + order.id + '/status',
      {status}, {headers: this.headers()}).subscribe(() => {
        this.busy = false;
        this.success = 'Order #' + order.id + ' updated to ' + status.toLowerCase() + '.';
        this.loadOrders();
        if (status === 'CANCELLED') { this.loadMenu(); }
        if (this.isAdmin) { this.loadDashboard(); }
      }, e => this.fail(e));
  }
  startAddFood(): void {
    this.editingFoodId = null;
    this.newFood = {name:'',category:'Vegetarian',description:'',price:99,stock:10};
    this.adminEditorOpen = true;
    this.error = '';
  }
  editFood(item: MenuItem): void {
    this.editingFoodId = item.id;
    this.newFood = {
      name: item.name, category: item.category, description: item.description || '',
      price: item.price, stock: item.stock
    };
    this.adminEditorOpen = true;
    this.error = '';
  }
  closeFoodEditor(): void {
    this.adminEditorOpen = false;
    this.editingFoodId = null;
    this.error = '';
  }
  saveFood(): void {
    if (!this.isAdmin || this.busy) { return; }
    const editing = this.editingFoodId !== null;
    const path = environment.apiUrl + '/menu' + (editing ? '/' + this.editingFoodId : '');
    this.busy = true; this.error = ''; this.success = '';
    const request = editing
      ? this.http.put(path, this.newFood, {headers: this.headers()})
      : this.http.post(path, this.newFood, {headers: this.headers()});
    request.subscribe(() => {
      this.busy = false;
      this.closeFoodEditor();
      this.success = editing ? 'Dish updated.' : 'Menu item added.';
      this.loadMenu(); this.loadHistory(); this.loadDashboard();
    }, e => this.fail(e));
  }
  updateStock(item: MenuItem): void {
    this.busy = true; this.error = '';
    this.http.patch(environment.apiUrl + '/menu/' + item.id + '/stock',
      {stock: Number(this.editingStock[item.id])}, {headers: this.headers()})
      .subscribe(() => { this.busy = false; this.success = 'Stock updated.'; this.loadMenu(); this.loadHistory(); this.loadDashboard(); }, e => this.fail(e));
  }
  removeFood(item: MenuItem): void {
    if (!confirm('Remove ' + item.name + ' from the menu?')) { return; }
    this.busy = true;
    this.http.delete(environment.apiUrl + '/menu/' + item.id, {headers: this.headers()})
      .subscribe(() => { this.busy = false; this.loadMenu(); this.loadHistory(); this.loadDashboard(); }, e => this.fail(e));
  }
  image(item: MenuItem): string {
    return item.imageUrl ? environment.apiUrl + item.imageUrl.replace(/^\/api/, '') :
      'assets/Images/' + (item.category === 'Drinks' ? 'coffee.jpg' :
        item.category === 'Non-vegetarian' ? 'hBurger.jpg' : 'pasta.jpg');
  }
}

import { Component, OnInit } from '@angular/core';
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
}
interface UserSession { accessToken: string; username: string; fullname: string; roles: string[]; }

@Component({
  selector: 'app-homepage',
  templateUrl: './homepage.component.html',
  styleUrls: ['./homepage.component.css']
})
export class HomepageComponent implements OnInit {
  menu: MenuItem[] = [];
  cart: CartItem[] = [];
  orders: CafeteriaOrder[] = [];
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
  activeTab = 'menu';
  private checkoutKey: string | null = null;

  constructor(private http: HttpClient, private auth: AuthService, private storage: TokenStorageService) {}

  ngOnInit(): void {
    this.restore();
    this.loadMenu();
    if (this.user) { this.loadOrders(); }
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
    this.http.get<MenuItem[]>(environment.apiUrl + '/menu').subscribe(
      data => {
        this.menu = data;
        data.forEach(item => this.editingStock[item.id] = item.stock);
        this.cart = this.cart.map(line => ({ item: data.find(f => f.id === line.item.id) || line.item, quantity: line.quantity }))
          .filter(line => data.some(f => f.id === line.item.id));
      }, e => this.fail(e));
  }
  login(): void {
    this.error = ''; this.success = ''; this.busy = true;
    this.auth.login(this.credentials).subscribe(data => {
      this.storage.saveToken(data.accessToken);
      this.storage.saveUser(data);
      this.user = data;
      this.credentials.password = '';
      this.busy = false;
      this.loadOrders();
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
    this.checkoutKey = null; this.activeTab = 'menu'; this.success = 'Signed out.';
  }
  add(item: MenuItem): void {
    if (!this.user || this.isStaff || item.stock <= 0) { return; }
    const current = this.cart.find(line => line.item.id === item.id);
    if (current) {
      if (current.quantity < item.stock) { current.quantity++; }
    } else { this.cart.push({ item, quantity: 1 }); }
    this.checkoutKey = null;
    this.success = '';
  }
  changeQty(line: CartItem, difference: number): void {
    line.quantity += difference;
    if (line.quantity <= 0) { this.cart = this.cart.filter(x => x !== line); }
    else { line.quantity = Math.min(line.quantity, line.item.stock); }
    this.checkoutKey = null;
  }
  checkout(): void {
    if (!this.user || !this.cart.length || this.unavailable || this.busy) { return; }
    this.busy = true; this.error = ''; this.success = '';
    if (!this.checkoutKey) { this.checkoutKey = 'order-' + Date.now() + '-' + Math.random().toString(36).slice(2); }
    const headers = this.headers().set('Idempotency-Key', this.checkoutKey);
    const items = this.cart.map(line => ({ foodId: line.item.id, quantity: line.quantity }));
    this.http.post<CafeteriaOrder>(environment.apiUrl + '/orders', {items}, {headers}).subscribe(order => {
      this.busy = false; this.cart = []; this.checkoutKey = null;
      this.success = 'Order #' + order.id + ' placed successfully.';
      this.activeTab = 'orders'; this.loadOrders(); this.loadMenu();
    }, e => this.fail(e));
  }
  loadOrders(): void {
    if (!this.user) { return; }
    const path = this.isStaff ? '/orders' : '/orders/mine';
    this.http.get<CafeteriaOrder[]>(environment.apiUrl + path, {headers: this.headers()})
      .subscribe(data => this.orders = data, e => this.fail(e));
  }
  nextStatus(status: string): string {
    return ({PLACED:'PREPARING', PREPARING:'READY', READY:'COMPLETED'} as {[key:string]:string})[status] || '';
  }
  advance(order: CafeteriaOrder): void {
    const status = this.nextStatus(order.status);
    if (!status) { return; }
    this.busy = true; this.error = '';
    this.http.patch<CafeteriaOrder>(environment.apiUrl + '/orders/' + order.id + '/status',
      {status}, {headers: this.headers()}).subscribe(() => { this.busy = false; this.loadOrders(); },
      e => this.fail(e));
  }
  createFood(): void {
    this.busy = true; this.error = '';
    this.http.post(environment.apiUrl + '/menu', this.newFood, {headers: this.headers()})
      .subscribe(() => {
        this.busy = false; this.adminEditorOpen = false; this.success = 'Menu item added.';
        this.newFood = {name:'',category:'Vegetarian',description:'',price:99,stock:10}; this.loadMenu();
      }, e => this.fail(e));
  }
  updateStock(item: MenuItem): void {
    this.busy = true; this.error = '';
    this.http.patch(environment.apiUrl + '/menu/' + item.id + '/stock',
      {stock: Number(this.editingStock[item.id])}, {headers: this.headers()})
      .subscribe(() => { this.busy = false; this.success = 'Stock updated.'; this.loadMenu(); }, e => this.fail(e));
  }
  removeFood(item: MenuItem): void {
    if (!confirm('Remove ' + item.name + ' from the menu?')) { return; }
    this.busy = true;
    this.http.delete(environment.apiUrl + '/menu/' + item.id, {headers: this.headers()})
      .subscribe(() => { this.busy = false; this.loadMenu(); }, e => this.fail(e));
  }
  image(item: MenuItem): string {
    return item.imageUrl ? environment.apiUrl + item.imageUrl.replace(/^\/api/, '') :
      'assets/Images/' + (item.category === 'Drinks' ? 'coffee.jpg' :
        item.category === 'Non-vegetarian' ? 'hBurger.jpg' : 'pasta.jpg');
  }
}

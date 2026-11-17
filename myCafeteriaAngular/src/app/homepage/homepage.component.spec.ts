import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { HomepageComponent } from './homepage.component';

describe('HomepageComponent', () => {
  let fixture: ComponentFixture<HomepageComponent>;
  let component: HomepageComponent;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, FormsModule],
      declarations: [HomepageComponent]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(HomepageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    http.expectOne('/api/menu').flush([]);
  });

  afterEach(() => http.verify());

  it('renders an empty menu without crashing', () => {
    expect(component).toBeTruthy();
    expect(component.filteredMenu.length).toBe(0);
  });

  it('calculates cart totals from item prices and quantities', () => {
    const item = {id:'1',name:'Chai',category:'Drinks',description:'Tea',price:25,stock:10};
    component.cart = [{item,quantity:3}];
    expect(component.total).toBe(75);
    expect(component.count).toBe(3);
    component.changeQty(component.cart[0],-1);
    expect(component.total).toBe(50);
  });
});

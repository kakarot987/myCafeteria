import { TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { HomepageComponent } from './homepage/homepage.component';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, FormsModule],
      declarations: [AppComponent, HomepageComponent]
    }).compileComponents();
  });

  it('renders the cafeteria interface', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/menu').flush([]);
    expect(fixture.nativeElement.textContent).toContain('Good food.');
    http.verify();
  });
});

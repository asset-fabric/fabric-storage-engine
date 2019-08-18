import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { NodetypeManagerComponent } from './nodetype-manager.component';

describe('NodetypeManagerComponent', () => {
  let component: NodetypeManagerComponent;
  let fixture: ComponentFixture<NodetypeManagerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ NodetypeManagerComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NodetypeManagerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

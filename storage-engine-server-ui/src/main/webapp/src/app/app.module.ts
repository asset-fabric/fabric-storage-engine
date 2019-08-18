import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HeaderContainerComponent } from './view/header-container/header-container.component';
import { TreeBrowserComponent } from './view/tree-browser/tree-browser.component';
import { SecurityManagerComponent } from './view/security-manager/security-manager.component';
import { NodetypeManagerComponent } from './view/nodetype-manager/nodetype-manager.component';
import { LoginComponent } from './view/login/login.component';
import {
  MatButtonModule,
  MatCardModule,
  MatFormFieldModule,
  MatIconModule,
  MatInputModule,
  MatTreeModule
} from "@angular/material";
import {FormsModule} from "@angular/forms";

@NgModule({
  declarations: [
    AppComponent,
    HeaderContainerComponent,
    TreeBrowserComponent,
    SecurityManagerComponent,
    NodetypeManagerComponent,
    LoginComponent
  ],
  imports: [
    AppRoutingModule,
    BrowserAnimationsModule,
    BrowserModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatTreeModule,
    MatCardModule,
    MatInputModule,
    MatFormFieldModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }

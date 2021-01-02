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
  MatInputModule, MatTableModule,
  MatTreeModule
} from "@angular/material";
import {FormsModule} from "@angular/forms";
import {HttpClientModule} from "@angular/common/http";
import { NodeInspectorComponent } from './view/node-inspector/node-inspector.component';

@NgModule({
  declarations: [
    AppComponent,
    HeaderContainerComponent,
    TreeBrowserComponent,
    SecurityManagerComponent,
    NodetypeManagerComponent,
    LoginComponent,
    NodeInspectorComponent
  ],
  imports: [
    AppRoutingModule,
    BrowserAnimationsModule,
    BrowserModule,
    FormsModule,
    HttpClientModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTreeModule,
    MatCardModule,
    MatInputModule,
    MatFormFieldModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }

import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import {HeaderContainerComponent} from "./view/header-container/header-container.component";
import {TreeBrowserComponent} from "./view/tree-browser/tree-browser.component";
import {SecurityManagerComponent} from "./view/security-manager/security-manager.component";
import {NodetypeManagerComponent} from "./view/nodetype-manager/nodetype-manager.component";
import {LoginComponent} from "./view/login/login.component";


const routes: Routes = [

  // the login screen
  { path: 'login', component: LoginComponent },

  // authenticated paths
  {
    path: '',
    component: HeaderContainerComponent,
    children: [
      // the search screen
      { path: 'browse', component: TreeBrowserComponent },
      { path: 'security', component: SecurityManagerComponent },
      { path: 'nodetypes', component: NodetypeManagerComponent }
    ]
  }

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }

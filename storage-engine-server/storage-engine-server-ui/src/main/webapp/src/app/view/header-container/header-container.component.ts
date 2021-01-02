import { Component, OnInit } from '@angular/core';
import {LoginModel} from "../../model/login.model";

@Component({
  selector: 'app-header-container',
  templateUrl: './header-container.component.html',
  styleUrls: ['./header-container.component.scss']
})
export class HeaderContainerComponent implements OnInit {

  constructor(private loginModel: LoginModel) { }

  ngOnInit() {
  }

}

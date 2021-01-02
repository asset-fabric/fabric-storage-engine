import { Component, OnInit } from '@angular/core';
import {LoginModel} from "../../model/login.model";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {

  constructor(private loginModel: LoginModel) { }

  ngOnInit() {
  }

}

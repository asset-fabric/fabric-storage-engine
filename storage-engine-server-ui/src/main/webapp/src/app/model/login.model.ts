import {Injectable} from "@angular/core";
import {Router} from "@angular/router";

@Injectable({providedIn: 'root'})
export class LoginModel {

  username: string;
  password: string;

  constructor(private router: Router) {

  }

  login() {
    this.router.navigate(['browse'], {replaceUrl: true});
  }

  clear() {

  }

}

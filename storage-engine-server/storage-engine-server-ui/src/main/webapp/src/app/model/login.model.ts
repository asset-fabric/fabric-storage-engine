import {Injectable} from "@angular/core";
import {Router} from "@angular/router";
import {SessionService} from "../service/session.service";

@Injectable({providedIn: 'root'})
export class LoginModel {

  username: string;
  password: string;

  constructor(private sessionService: SessionService, private router: Router) {
    sessionService.sessionSubject.subscribe(next => {
      this.password = null;
      if (next) {
        this.router.navigate(['browse'], {replaceUrl: true});
      } else {
        this.router.navigate(['login'], {replaceUrl: true});
      }
    })
  }

  login() {
    this.sessionService.login(this.username, this.password);
  }

  logout() {
    this.router.navigate(['login'], {replaceUrl: true});
  }

  clear() {

  }

}

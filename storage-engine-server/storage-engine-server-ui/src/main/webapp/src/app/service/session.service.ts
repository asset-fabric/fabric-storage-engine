import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {BehaviorSubject} from "rxjs";

@Injectable({providedIn: 'root'})
export class SessionService {

  private client: HttpClient;

  public sessionSubject = new BehaviorSubject<boolean>(false);

  constructor(private httpClient: HttpClient) {
    this.client = httpClient;
  }

  login(username: String, password: String) {
    const res = this.client.post("/v1/session", {
      "username": username,
      "password": password
    });

    res.subscribe( next => {
      this.sessionSubject.next(true);
    }, error1 => {
      this.sessionSubject.error(error1);
    });

  }

  logout() {

  }
}

import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {NodeModel} from "../model/entity/node.model";
import {Observable} from "rxjs";
import {map} from "rxjs/operators";
import {StreamingHttpClient} from "../util/StreamingHttpClient";

@Injectable({providedIn: 'root'})
export class NodeService {

  constructor(private httpClient: HttpClient, private streamingClient: StreamingHttpClient) { }

  getNode(path: String): Observable<NodeModel> {
    let reqUrl = `/v1/node?path=${path}`;
    console.info("Getting node from " + reqUrl);
    return this.httpClient.get(reqUrl).pipe(map(data =>  { return NodeModel.nodeFromObject(data) }));
  }

  getChildren(path: String, childFunction: (child: NodeModel) => void, doneFunction: () => void) {
    this.streamingClient.get(`/v1/node/children?path=${path}`, childFunction, doneFunction);
  }

}

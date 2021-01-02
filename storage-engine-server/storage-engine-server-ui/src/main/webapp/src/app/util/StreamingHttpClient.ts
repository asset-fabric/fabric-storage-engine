import Oboe from 'oboe';
import {Injectable} from "@angular/core";
import {NodeModel} from "../model/entity/node.model";

@Injectable({providedIn: 'root'})
export class StreamingHttpClient {

  /**
   * Gets streaming data from a URL.
   * @param url the url from which to retrieve streaming data
   * @param nextFunction the function to call on each received streamed object
   * @param completeFunction the function to run when all objects have been streamed
   */
  get(url: String, nextFunction: Function, completeFunction: Function) {
    Oboe({url: url, method: "GET"})
      .node("*", function(obj) {
        // TODO: remove this?
        /*
        // check to make sure we actually got a response...
        if (obj["path"]) {
          let p = NodeModel.nodeFromObject(obj);
          console.info("returning object...");
          console.info(p);
          //nextFunction(p);
        }
        */
    }).done(next => {
      // TODO: can we just use this??
      let p = NodeModel.nodeFromObject(next);
      console.info(p);
      nextFunction(p);
    }).on('end', () => completeFunction());

  }

}



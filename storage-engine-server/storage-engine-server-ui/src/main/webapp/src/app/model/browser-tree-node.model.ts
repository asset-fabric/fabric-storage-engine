import {Observable} from "rxjs";
import {NodePropertyModel} from "./entity/node-property.model";

export abstract class BrowserTreeNodeModel {

  abstract name(): String;

  abstract path(): String;

  abstract nodeType(): String;

  abstract properties(): Map<string, NodePropertyModel>;

  abstract children(): Observable<BrowserTreeNodeModel[]>;

  abstract areChildrenLoaded(): Boolean;

  abstract isExpandable(): Observable<Boolean>;

  abstract hasChildren(): Boolean;

  abstract loadChildren();

}

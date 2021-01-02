import {BrowserTreeNodeModel} from "../browser-tree-node.model";
import {NodeModel} from "../entity/node.model";
import {NodeService} from "../../service/node.service";
import {BehaviorSubject, Observable} from "rxjs";
import {NodePropertyModel} from "../entity/node-property.model";

export class DefaultBrowserTreeNodeModel extends BrowserTreeNodeModel {

  private _children: DefaultBrowserTreeNodeModel[] = [];
  private _childrenObs = new BehaviorSubject<BrowserTreeNodeModel[]>(this._children);
  private expandableSubject = new BehaviorSubject<Boolean>(true);
  private childrenLoaded: Boolean = false;

  constructor(private node: NodeModel, private nodeService: NodeService) {
    super();
  }

  hasChildren(): Boolean {
    return this._children.length > 0;
  }

  children(): Observable<BrowserTreeNodeModel[]> {
    return this._childrenObs;
  }

  isExpandable(): Observable<Boolean> {
    return this.expandableSubject;
  }

  name(): String {
    return this.node.name == "" ? "(root)" : this.node.name;
  }

  path(): String {
    return this.node.path;
  }

  nodeType(): String {
    return this.node.nodeType;
  }

  properties(): Map<string, NodePropertyModel> {
    console.info(this.node);
    return this.node.properties;
  }

  areChildrenLoaded(): Boolean {
    return this.childrenLoaded;
  }

  loadChildren() {
    if (!this.childrenLoaded) {
      console.info(`Load children using ${this.nodeService}`);
      let self = this;
      let doneFunction = () => {
        self.childrenLoaded = true;
        self._childrenObs.next(self._children);
        this.expandableSubject.next(this.hasChildren());
      };
      this.nodeService.getChildren(this.node.path, (child: NodeModel) => {
        console.info(child);
        let newChild = new DefaultBrowserTreeNodeModel(child, this.nodeService);
        // for each child, if its expansion ability changes, we need to push out an update
        // event so that this node's children will be re-rendered in the tree.
        newChild.expandableSubject.subscribe(next => {
          this._childrenObs.next([]);
          this._childrenObs.next(this._children);
        });
        this._children.push(newChild);
      }, doneFunction);
    }
  }

}

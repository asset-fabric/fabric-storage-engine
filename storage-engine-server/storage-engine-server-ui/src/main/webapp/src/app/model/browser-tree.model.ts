import {MatTreeNestedDataSource} from "@angular/material";
import {BrowserTreeNodeModel} from "./browser-tree-node.model";
import {NodeService} from "../service/node.service";
import {NodeModel} from "./entity/node.model";
import {DefaultBrowserTreeNodeModel} from "./support/default-browser-tree-node.model";
import {Injectable} from "@angular/core";

@Injectable({providedIn: 'root'})
export class BrowserTreeModel extends MatTreeNestedDataSource<BrowserTreeNodeModel> {

  currentSelectedNode: BrowserTreeNodeModel = null;

  getSelectedNode(): BrowserTreeNodeModel {
    return this.currentSelectedNode;
  }

  select(node: BrowserTreeNodeModel) {
    this.currentSelectedNode = node;
  }

  constructor(private nodeService: NodeService) {
    super();
  }

  load() {
    let sub = this.nodeService.getNode("/");
    let obs = sub.subscribe((next: NodeModel) => {
      this.data = [new DefaultBrowserTreeNodeModel(next, this.nodeService)];
      obs.unsubscribe();
    });
  }

}

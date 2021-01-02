import { Component, OnInit } from '@angular/core';
import {NestedTreeControl} from "@angular/cdk/tree";
import {NodeService} from "../../service/node.service";
import {BrowserTreeNodeModel} from "../../model/browser-tree-node.model";
import {BrowserTreeModel} from "../../model/browser-tree.model";

@Component({
  selector: 'app-tree-browser',
  templateUrl: './tree-browser.component.html',
  styleUrls: ['./tree-browser.component.scss']
})
export class TreeBrowserComponent implements OnInit {

  treeControl = new NestedTreeControl<BrowserTreeNodeModel>(node => node.children());

  constructor(private nodeService: NodeService, private treeModel: BrowserTreeModel) {

  }

  ngOnInit(): void {
    this.treeModel.load();
    this.treeControl.expansionModel.changed.subscribe(selectionChange => {
      selectionChange.added.forEach(model => model.loadChildren());
    });
  }

  setSelectedNode(event: MouseEvent, node: BrowserTreeNodeModel) {
    event.preventDefault();
    event.stopPropagation();
    console.info(`selected node ${node.name()}`);
    this.treeModel.select(node);
  }

  selectedNode(): BrowserTreeNodeModel {
    return this.treeModel.getSelectedNode();
  }


  hasChild = (_: number, node: BrowserTreeNodeModel) => {
    return !node.areChildrenLoaded() || node.hasChildren();
  }

}

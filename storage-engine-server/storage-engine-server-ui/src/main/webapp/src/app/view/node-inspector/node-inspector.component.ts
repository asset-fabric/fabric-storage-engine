import {Component, Input, OnInit} from '@angular/core';
import {BrowserTreeNodeModel} from "../../model/browser-tree-node.model";
import {SingleValueNodePropertyModel} from "../../model/entity/single-value-node-property.model";
import {ListValueNodePropertyModel} from "../../model/entity/list-value-node-property.model";


export interface NodeProperty {
  name: string;
  type: string;
  value: any;
}

@Component({
  selector: 'app-node-inspector',
  templateUrl: './node-inspector.component.html',
  styleUrls: ['./node-inspector.component.scss']
})
export class NodeInspectorComponent implements OnInit {

  displayedColumns: string[] = ['propertyName', 'propertyType', 'propertyValue'];
  dataSource: NodeProperty[] = [];
  _node: BrowserTreeNodeModel;

  @Input()
  set node(n: BrowserTreeNodeModel) {
    if (n) {
      this._node = n;
      const nodeProperties = this._node.properties();
      const props: NodeProperty[] = [];
      for (let [key, nodeProp] of nodeProperties) {
        let nodeVal = nodeProp instanceof SingleValueNodePropertyModel ? (nodeProp as SingleValueNodePropertyModel).value : (nodeProp as ListValueNodePropertyModel).values;
        const prop = {
          name: key,
          type: nodeProp.type,
          value: nodeVal
        };
        props.push(prop);
      };
      this.dataSource = props;
      console.info(this.dataSource);
    }
  }

  get node() {
   return this._node;
  }

  constructor() {
  }

  ngOnInit() {
  }

}

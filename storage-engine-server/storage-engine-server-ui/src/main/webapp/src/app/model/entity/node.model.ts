import {NodePropertyModel} from "./node-property.model";
import {ListValueNodePropertyModel} from "./list-value-node-property.model";
import {SingleValueNodePropertyModel} from "./single-value-node-property.model";

export class NodeModel {

  name: string;
  path: string;
  nodeType: string;
  properties: Map<string, NodePropertyModel> = new Map<string, NodePropertyModel>();

  static nodeFromObject(obj: any): NodeModel {
    let model = new NodeModel();
    model.name = obj["name"];
    model.path = obj["path"];
    model.nodeType = obj["nodeType"];
    const props = obj["properties"];
    if (props) {
      Object.keys(props).forEach(propertyName => {
        const propertyDef = props[propertyName];
        const propertyType = propertyDef["type"];
        let propModel: NodePropertyModel;
        if (propertyType.endsWith("[]")) {
          propModel = new ListValueNodePropertyModel();
          propModel.type = propertyType;
          (propModel as ListValueNodePropertyModel).values = propertyDef["values"];
        } else {
          propModel = new SingleValueNodePropertyModel();
          propModel.type = propertyType;
          (propModel as SingleValueNodePropertyModel).value = propertyDef["value"];
        }
        model.properties.set(propertyName, propModel);
      });
    } else {
      console.info("Node properties not found");
    }
    return model;
  }

}

package de.catma.ui.tagger.client.ui;

import java.util.List;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

public class DebugUtil {
	
	public static void printNodes(String listName, List<Node> nodeList) {
		System.out.println(listName + ":");
		for (Node n : nodeList) {
			printNode(n);
		}
	}
	 
	public static void printNode(Node node) {
		if (node == null) {
			System.out.println(node);
		}
		if (Element.is(node)) {
			Element e = Element.as(node);
			System.out.println(
					node.getNodeName() + 
					"#"+ e.getId());
		}
		else {
			System.out.println(node.getNodeName() + "[" + node.getNodeValue() + "]");
		}
	}
}

package de.catma.ui.tagger.client.ui;

import java.util.List;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.vaadin.terminal.gwt.client.VConsole;

public class DebugUtil {
	
	public static void printNodes(String listName, List<Node> nodeList) {
		println(listName + ":");
		for (Node n : nodeList) {
			printNode(n);
		}
	}
	 
	public static void printNode(Node node) {
		if (node == null) {
			println("null");
		}
		if (Element.is(node)) {
			Element e = Element.as(node);
			println(
					node.getNodeName() + 
					"#"+ e.getId());
		}
		else {
			println(node.getNodeName() + "[" + node.getNodeValue() + "]");
		}
	}
	
	private static void println(String s) {
//		System.out.println(s);
		VConsole.log(s);
	}
}

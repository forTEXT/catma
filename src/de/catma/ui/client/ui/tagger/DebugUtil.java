package de.catma.ui.client.ui.tagger;

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
			println( "Element " +
					node.getNodeName() + 
					"#"+ e.getId());
		}
		else {
			println(node.getNodeName() + "[" + node.getNodeValue() + "]");
		}
	}

	public static String getNodeInfo(Node node) {
		if (node == null) {
			return "null";
		}
		if (Element.is(node)) {
			Element e = Element.as(node);
			return "Element " +
					node.getNodeName() + 
					"#"+ e.getId();
		}
		else {
			return node.getNodeName() + "[" + node.getNodeValue() + "]";
		}
	}
	
	private static void println(String s) {
//		System.out.println(s);
		VConsole.log(s);
	}
}

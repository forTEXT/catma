/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2012  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */   
package de.catma.ui.client.ui.tagger;

import java.util.List;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.vaadin.terminal.gwt.client.VConsole;

/**
 * @author marco.petris@web.de
 *
 */
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

package de.catma.ui.client.ui.tagmanager;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.DOM;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.ui.VTree;

public class VTagTree extends VTree {
	
	private static final String TREENODE_CLASS_PATTERN = "v-tree-node-catma-tag-color-";
	private static final String TREENODE_CAPTION_CLASS_PATTERN = "v-tree-node-caption-catma-tag-color-";
	private static final String TREENODE_CHILDREN_CLASS_PATTERN = "v-tree-node-children-catma-tag-color-";
	
	public VTagTree() {
	}

	@Override
	public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
		super.updateFromUIDL(uidl, client);
        
		if (client.updateComponent(this, uidl, true)) {
            return;
        }
		
		List<Element> treeNodes = getTagTreeNodes();
		addTagColorBox(treeNodes);
	}
	
	private void addTagColorBox(List<Element> treeNodes) {
		for (Element treeNode : treeNodes) {
			
			NodeList<Element> spans = treeNode.getElementsByTagName("span");
			if (spans.getLength() == 1) {
				Element span = spans.getItem(0);
				Element colorSpan = DOM.createSpan();
				String className = treeNode.getClassName();
				int start = 
						className.indexOf(
								TREENODE_CLASS_PATTERN)+TREENODE_CLASS_PATTERN.length();
				int end = className.indexOf(" ", start);
				if (end == -1) {
					end = className.length();
				}
				String colorValue = className.substring(start, end);
				colorSpan.setAttribute(
						"style", "background-color:#"+colorValue+";margin-left:3px;");
				colorSpan.setInnerHTML("&nbsp;&nbsp;&nbsp;&nbsp;");
				span.getParentElement().appendChild(colorSpan);

				treeNode.setClassName(className.replace(TREENODE_CLASS_PATTERN+colorValue, ""));
			}
		}
	}

	private List<Element> getTagTreeNodes() {
		NodeList<Element> descendents = getElement().getElementsByTagName("div");
		ArrayList<Element> result = new ArrayList<Element>();
		
		for (int i=0; i<descendents.getLength(); i++) {
			Element current = descendents.getItem(i);
			String className = current.getClassName();
			if (className != null) {
				if (className.contains(TREENODE_CLASS_PATTERN)) {
					result.add(current);
				}
				else if (className.contains(TREENODE_CAPTION_CLASS_PATTERN)) {
					int start = className.indexOf(TREENODE_CAPTION_CLASS_PATTERN);
					current.setClassName(
						className.replace(
							className.substring(start, start+TREENODE_CAPTION_CLASS_PATTERN.length()+6), ""));
				}
				else if (className.contains(TREENODE_CHILDREN_CLASS_PATTERN)) {
					int start = className.indexOf(TREENODE_CHILDREN_CLASS_PATTERN);
					current.setClassName(
						className.replace(
							className.substring(start, start+TREENODE_CHILDREN_CLASS_PATTERN.length()+6), ""));
				}
			}
		}
		
		return result;
	}
}

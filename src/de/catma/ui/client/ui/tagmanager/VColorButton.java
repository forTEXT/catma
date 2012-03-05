package de.catma.ui.client.ui.tagmanager;

import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HTML;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.ui.VButton;


public class VColorButton extends VButton {
	
	public static final String COLOR_ATTRIBUTE = "COLOR_ATTRIBUTE";
	private Element colorElement;
	private String color = "";
	
	public VColorButton() {
	}

	
	@Override
	public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
		super.updateFromUIDL(uidl, client);
		
		if (uidl.hasAttribute(COLOR_ATTRIBUTE)) {
			if (!this.color.equals(uidl.getStringAttribute(COLOR_ATTRIBUTE))) {
				HTML html = new HTML(SafeHtmlUtils.fromTrustedString(
						"<span style=\"background-color:#"
						+ uidl.getStringAttribute(COLOR_ATTRIBUTE)
						+ ";margin-left:3px;\">&nbsp;&nbsp;&nbsp;&nbsp;</span>"));
				if (colorElement == null) {
					wrapper.appendChild(html.getElement());
				}
				else {
					wrapper.replaceChild(html.getElement(), colorElement);
				}
				colorElement = html.getElement();
				color = uidl.getStringAttribute(COLOR_ATTRIBUTE);
			}
		}
	}
}

/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
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

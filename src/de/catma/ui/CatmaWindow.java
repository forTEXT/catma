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
package de.catma.ui;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.ui.ClientWidget;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Window;

import de.catma.ui.client.ui.VCatmaWindow;

@ClientWidget(VCatmaWindow.class)
public class CatmaWindow extends Window {
	
	private boolean enableScrolling = false;
	private boolean stayOnTop = false;
	
	private Map<String,String> attributes = new HashMap<String, String>();
	
	public CatmaWindow() {
		super();
	}

	public CatmaWindow(String caption, ComponentContainer content) {
		super(caption, content);
	}

	public CatmaWindow(String caption) {
		super(caption);
	}

	public void setPosition() {
		center();
	}
	
	@Override
	public synchronized void paintContent(PaintTarget target)
			throws PaintException {
		super.paintContent(target);
		
		for (Map.Entry<String, String> entry : attributes.entrySet()) {
			target.addAttribute(entry.getKey(), entry.getValue());
		}
		
		attributes.clear();
	}
	
	public void setEnableScrolling(boolean enableScrolling) {
		this.enableScrolling = enableScrolling;
		attributes.put(
				VCatmaWindow.EventAttribute.enableScrolling.name(), 
				String.valueOf(enableScrolling));
	}
	
	public boolean isEnableScrolling() {
		return enableScrolling;
	}
	
	public void setStayOnTop(boolean stayOnTop) {
		this.stayOnTop = stayOnTop;
		attributes.put(
				VCatmaWindow.EventAttribute.stayOnTop.name(), 
				String.valueOf(stayOnTop));
	}
	
	public boolean isStayOnTop() {
		return stayOnTop;
	}
	
}

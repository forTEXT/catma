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
package de.catma.ui.tagmanager;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

public class PanelStateManager implements ValueChangeListener {
	
	private VerticalLayout propertyPanel;
	
	private boolean withPropertyButtons;

	public PanelStateManager(
			VerticalLayout propertyPanel,
			boolean withPropertyButtons
			) {
		this.propertyPanel = propertyPanel;
		this.withPropertyButtons = withPropertyButtons;
		tagsetDefSelected(false);
	}



	public void valueChange(ValueChangeEvent event) {
		TreeTable treeTable = (TreeTable)event.getProperty();
		
		Object value = treeTable.getValue();
				
		if (value == null) {
			tagsetDefSelected(false); // all disabled
		}
		else if (value instanceof TagsetDefinition) {
			tagsetDefSelected(true);
		}
		else if (value instanceof TagDefinition) {
			tagDefSelected(true);
		}
		else if (value instanceof PropertyDefinition) {
			propDefSelected(true);
		}
		else {
			tagsetDefSelected(false); // all disabled
		}
		
	}
	
	private void tagsetDefSelected(boolean selected) {	
		if (withPropertyButtons) {
			propertyPanel.setVisible(false);
		}
	}
	
	private void tagDefSelected(boolean selected) {
		if (withPropertyButtons && selected) {
			propertyPanel.setVisible(true);			
		}
	}
	
	private void propDefSelected(boolean selected) {	
		if (withPropertyButtons) {
			propertyPanel.setVisible(true);
		}
	}	

}

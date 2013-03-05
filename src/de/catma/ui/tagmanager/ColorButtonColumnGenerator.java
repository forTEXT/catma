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

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;

import de.catma.tag.TagDefinition;
import de.catma.util.ColorConverter;

public class ColorButtonColumnGenerator implements ColumnGenerator {
	
	public static interface ColorButtonListener {
		public void colorButtonClicked(TagDefinition tagDefinition);
		public void setEnabled(boolean enabled);
	}

	private ColorButtonListener colorButtonListener;
	
	public ColorButtonColumnGenerator(ColorButtonListener colorButtonListener) {
		this.colorButtonListener = colorButtonListener;
	}

	public Object generateCell(
			Table source, final Object itemId, Object columnId) {
		if (itemId instanceof TagDefinition) {
			ColorButton colorButton = new ColorButton(
				ColorConverter.toHex(((TagDefinition)itemId).getColor()), 
				new ClickListener() {
				
					public void buttonClick(ClickEvent event) {
						if (itemId instanceof TagDefinition) {
							colorButtonListener.colorButtonClicked(
									(TagDefinition)itemId);
						}
					}
			});
			
			return colorButton;
		}
		
		return new Label();
	}

}

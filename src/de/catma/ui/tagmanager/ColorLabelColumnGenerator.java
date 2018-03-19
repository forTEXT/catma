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

import java.text.MessageFormat;

import com.vaadin.v7.ui.Label;
import com.vaadin.v7.ui.Table;
import com.vaadin.v7.ui.Table.ColumnGenerator;

import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.util.ColorConverter;

public class ColorLabelColumnGenerator implements ColumnGenerator {
	
	public static final String COLORLABEL_HTML = 
			"<span style=\"background-color:#{0};margin-left:3px;\">" + //$NON-NLS-1$
					"&nbsp;&nbsp;&nbsp;&nbsp;" + //$NON-NLS-1$
			"</span>"; //$NON-NLS-1$
	
	public static interface TagDefinitionProvider {
		public TagDefinition getTagDefinition(Object itemId);
	}
	
	public static interface ColorProvider {
		public String getColor(Object itemId);		
	}
	
	public static class DefaultTagDefinitionProvider implements TagDefinitionProvider {
		public TagDefinition getTagDefinition(Object itemId) {
			if (itemId instanceof TagDefinition) {
				return (TagDefinition)itemId;
			}
			return null;
		}
	}
	
	public static class TagInstanceTagDefinitionProvider implements TagDefinitionProvider {
		public TagDefinition getTagDefinition(Object itemId) {
			if (itemId instanceof TagInstance) {
				return ((TagInstance)itemId).getTagDefinition();
			}
			return null;
		}
	}
	
	public static class TagDefinitionColorProvider implements ColorProvider {
		private TagDefinitionProvider tagDefinitionProvider;

		public TagDefinitionColorProvider(
				TagDefinitionProvider tagDefinitionProvider) {
			this.tagDefinitionProvider = tagDefinitionProvider;
		}
		
		public String getColor(Object itemId) {
			TagDefinition td = tagDefinitionProvider.getTagDefinition(itemId);
			if (td != null) {
				return td.getColor();
			}
			
			return null;
		}
	}
	
	private ColorProvider colorProvider;
	
	public ColorLabelColumnGenerator() {
		this(new DefaultTagDefinitionProvider());
	}

	public ColorLabelColumnGenerator(TagDefinitionProvider tagDefinitionProvider) {
		this(new TagDefinitionColorProvider(tagDefinitionProvider));
	}
	
	public ColorLabelColumnGenerator(ColorProvider colorProvider) {
		this.colorProvider = colorProvider;
	}

	public Object generateCell(Table source, Object itemId, Object columnId) {
		try {
			String color = colorProvider.getColor(itemId);
			
			if (color != null) {
				Label colorLabel = 
					new Label(
						MessageFormat.format(
							COLORLABEL_HTML, 
							ColorConverter.toHex((
								color))));
				colorLabel.setContentMode(Label.CONTENT_XHTML);
				return colorLabel;
			}
		}
		catch (RuntimeException e) {
			e.printStackTrace();//TODO: log
			throw e;
		}
		return new Label();
	}

}

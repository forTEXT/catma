package de.catma.ui.tagmanager;

import java.text.MessageFormat;

import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;

import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.util.ColorConverter;

public class ColorLabelColumnGenerator implements ColumnGenerator {
	
	private static final String COLORLABEL_HTML = 
			"<span style=\"background-color:#{0};margin-left:3px;\">" +
					"&nbsp;&nbsp;&nbsp;&nbsp;" +
			"</span>";
	
	public static interface TagDefinitionProvider {
		public TagDefinition getTagDefinition(Object itemId);
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
	
	private TagDefinitionProvider tagDefinitionProvider;
	
	public ColorLabelColumnGenerator() {
		this(new DefaultTagDefinitionProvider());
	}

	public ColorLabelColumnGenerator(TagDefinitionProvider tagDefinitionProvider) {
		this.tagDefinitionProvider = tagDefinitionProvider;
	}

	public Object generateCell(Table source, Object itemId, Object columnId) {
		TagDefinition td = tagDefinitionProvider.getTagDefinition(itemId);
		
		if (td != null) {
			Label colorLabel = 
				new Label(
					MessageFormat.format(
						COLORLABEL_HTML, 
						ColorConverter.toHex((
							td.getColor()))));
			colorLabel.setContentMode(Label.CONTENT_XHTML);
			return colorLabel;
		}
		
		return new Label();
	}

}

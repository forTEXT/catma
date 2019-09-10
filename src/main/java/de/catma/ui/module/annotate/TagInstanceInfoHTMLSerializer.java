package de.catma.ui.module.annotate;

import java.util.List;

import de.catma.document.annotation.Annotation;
import de.catma.project.Project;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import nu.xom.Attribute;
import nu.xom.Element;

public class TagInstanceInfoHTMLSerializer {

	private Project project;

	public TagInstanceInfoHTMLSerializer(Project project) {
		this.project = project;
	}

	public String toHTML(Annotation tagInstanceInfo) {
		
		Element table = new Element("table"); //$NON-NLS-1$
		table.addAttribute(new Attribute("class", "taginstanceinfo")); //$NON-NLS-1$ //$NON-NLS-2$
		
		Element tagPathField = new Element("td"); //$NON-NLS-1$
		tagPathField.addAttribute(new Attribute("colspan", "2")); //$NON-NLS-1$ //$NON-NLS-2$
		tagPathField.addAttribute(new Attribute("class", "taginstanceinfo-caption")); //$NON-NLS-1$ //$NON-NLS-2$
		tagPathField.appendChild(tagInstanceInfo.getTagPath());
		addRow(table, tagPathField, ""); //$NON-NLS-1$
		addRow(table, "Collection", tagInstanceInfo.getUserMarkupCollection().getName()); //$NON-NLS-1$
		
		TagInstance tagInstance = tagInstanceInfo.getTagInstance();
		String tagId = tagInstance.getTagDefinitionId();
		String author = tagInstance.getAuthor();
		TagDefinition tag = project.getTagManager().getTagLibrary().getTagDefinition(tagId);
		
		addRow(table, "Author", author); //$NON-NLS-1$
		
		if (!tagInstance.getUserDefinedProperties().isEmpty()) {
			Element propertyField = new Element("td"); 
			propertyField.addAttribute(new Attribute("class", "taginstanceinfo-caption")); //$NON-NLS-1$ //$NON-NLS-2$
			propertyField.appendChild("Properties"); 
			addRow(table, propertyField, ""); //$NON-NLS-1$
			
			for (Property property : tagInstance.getUserDefinedProperties()) {
				List<String> values = property.getPropertyValueList();
				PropertyDefinition propertyDefinition = 
						tag.getPropertyDefinitionByUuid(property.getPropertyDefinitionId());
				if (propertyDefinition != null) { // may be deleted already
					addRow(
							table, 
							tag.getPropertyDefinitionByUuid(property.getPropertyDefinitionId()).getName(), 
							values.isEmpty()?"":values.size() > 1?values.toString():values.get(0)); //$NON-NLS-1$
				}
			}
		}		
		
		return table.toXML();
	}

	private void addRow(Element parent, String key, String value) {
		Element keyField = new Element("td"); //$NON-NLS-1$
		keyField.appendChild(key);
		addRow(parent, keyField, value);
	}
	
	private void addRow(Element parent, Element keyField, String value) {
		Element row = new Element("tr"); //$NON-NLS-1$
		parent.appendChild(row);
		row.appendChild(keyField);
		Element valueField = new Element("td"); //$NON-NLS-1$
		row.appendChild(valueField);
		valueField.appendChild(value);
	}
}

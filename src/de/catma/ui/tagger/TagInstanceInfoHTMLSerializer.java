package de.catma.ui.tagger;

import java.util.List;

import de.catma.document.standoffmarkup.usermarkup.TagInstanceInfo;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition.SystemPropertyName;
import de.catma.tag.TagInstance;
import nu.xom.Attribute;
import nu.xom.Element;

public class TagInstanceInfoHTMLSerializer {

	public String toHTML(TagInstanceInfo tagInstanceInfo) {
		
		Element table = new Element("table"); //$NON-NLS-1$
		table.addAttribute(new Attribute("class", "taginstanceinfo")); //$NON-NLS-1$ //$NON-NLS-2$
		
		Element tagPathField = new Element("td"); //$NON-NLS-1$
		tagPathField.addAttribute(new Attribute("colspan", "2")); //$NON-NLS-1$ //$NON-NLS-2$
		tagPathField.addAttribute(new Attribute("class", "taginstanceinfo-caption")); //$NON-NLS-1$ //$NON-NLS-2$
		tagPathField.appendChild(tagInstanceInfo.getTagPath());
		addRow(table, tagPathField, ""); //$NON-NLS-1$
		addRow(table, "Collection", tagInstanceInfo.getUserMarkupCollection().getName()); //$NON-NLS-1$
		
		TagInstance tagInstance = tagInstanceInfo.getTagInstance();
		
		String author = tagInstance.getSystemProperty(
			tagInstance.getTagDefinition().getPropertyDefinitionByName(
				SystemPropertyName.catma_markupauthor.name()).getUuid()).getPropertyValueList().getFirstValue();
		addRow(table, "Author", author); //$NON-NLS-1$
		
		if (!tagInstance.getUserDefinedProperties().isEmpty()) {
			Element propertyField = new Element(Messages.getString("TagInstanceInfoHTMLSerializer.11")); //$NON-NLS-1$
			propertyField.addAttribute(new Attribute("class", "taginstanceinfo-caption")); //$NON-NLS-1$ //$NON-NLS-2$
			propertyField.appendChild(Messages.getString("TagInstanceInfoHTMLSerializer.14")); //$NON-NLS-1$
			addRow(table, propertyField, ""); //$NON-NLS-1$
			
			for (Property property : tagInstance.getUserDefinedProperties()) {
				List<String> values = property.getPropertyValueList().getValues();
				
				addRow(table, property.getName(), values.isEmpty()?"":values.size() > 1?values.toString():values.get(0)); //$NON-NLS-1$
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

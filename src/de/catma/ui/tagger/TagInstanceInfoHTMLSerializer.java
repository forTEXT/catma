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
		
		Element table = new Element("table");
		table.addAttribute(new Attribute("class", "taginstanceinfo"));
		
		Element tagPathField = new Element("td");
		tagPathField.addAttribute(new Attribute("colspan", "2"));
		tagPathField.addAttribute(new Attribute("class", "taginstanceinfo-caption"));
		tagPathField.appendChild(tagInstanceInfo.getTagPath());
		addRow(table, tagPathField, "");
		addRow(table, "Collection", tagInstanceInfo.getUserMarkupCollection().getName());
		
		TagInstance tagInstance = tagInstanceInfo.getTagInstance();
		
		String author = tagInstance.getSystemProperty(
			tagInstance.getTagDefinition().getPropertyDefinitionByName(
				SystemPropertyName.catma_markupauthor.name()).getUuid()).getPropertyValueList().getFirstValue();
		addRow(table, "Author", author);
		
		if (!tagInstance.getUserDefinedProperties().isEmpty()) {
			Element propertyField = new Element("td");
			propertyField.addAttribute(new Attribute("class", "taginstanceinfo-caption"));
			propertyField.appendChild("Properties");
			addRow(table, propertyField, "");
			
			for (Property property : tagInstance.getUserDefinedProperties()) {
				List<String> values = property.getPropertyValueList().getValues();
				
				addRow(table, property.getName(), values.isEmpty()?"":values.size() > 1?values.toString():values.get(0));
			}
		}		
		
		return table.toXML();
	}

	private void addRow(Element parent, String key, String value) {
		Element keyField = new Element("td");
		keyField.appendChild(key);
		addRow(parent, keyField, value);
	}
	
	private void addRow(Element parent, Element keyField, String value) {
		Element row = new Element("tr");
		parent.appendChild(row);
		row.appendChild(keyField);
		Element valueField = new Element("td");
		row.appendChild(valueField);
		valueField.appendChild(value);
	}
}

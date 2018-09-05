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
package de.catma.serialization.tei;

import java.io.IOException;
import java.text.ParseException;

import de.catma.ExceptionHandler;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import nu.xom.Elements;
import nu.xom.Nodes;

public class TeiTagLibraryDeserializer {
	
	private TeiDocument teiDocument;
	private TagLibrary tagLibrary;
	private TagManager tagManager;

	public TeiTagLibraryDeserializer(
			TeiDocument teiDocument, TagManager tagManager) throws IOException {
		super();
		this.teiDocument = teiDocument;
		this.tagManager = tagManager;
		this.tagLibrary = new TagLibrary(
				teiDocument.getId(), teiDocument.getName());
		try {
			deserialize();
		} catch (ParseException e) {
			throw new IOException(e);
		}
	}

	private void deserialize() throws ParseException {
		Nodes tagsetDefinitionElements = teiDocument.getTagsetDefinitionElements();
		
		for (int i=0; i<tagsetDefinitionElements.size(); i++) {
			TeiElement tagsetDefinitionElement = (TeiElement)tagsetDefinitionElements.get(i);
			String nValue = tagsetDefinitionElement.getAttributeValue(Attribute.n);
			int dividerPos = nValue.lastIndexOf(' ');
			String tagsetName = nValue.substring(0, dividerPos);
			String versionString = nValue.substring(dividerPos+1);

			TagsetDefinition tagsetDefinition = 
					new TagsetDefinition(
							null,
							tagsetDefinitionElement.getID(),tagsetName, 
							new Version(versionString));
			
			addTagDefinitions(tagsetDefinition, tagsetDefinitionElement.getChildElements(TeiElementName.fsDecl));

			tagManager.addTagsetDefinition(tagLibrary, tagsetDefinition);
		}
	}

	private void addTagDefinitions(
			TagsetDefinition tagsetDefinition, Elements tagDefinitionElements) throws ParseException {
		
		for (int i=0; i<tagDefinitionElements.size(); i++) {
			TeiElement tagDefinitionElement = (TeiElement)tagDefinitionElements.get(i);
			TeiElement descriptionElement = 
					tagDefinitionElement.getFirstTeiChildElement(TeiElementName.fsDescr);
			
			String description = "";
			
			if ((descriptionElement != null) && (descriptionElement != null)) {
				description = descriptionElement.getValue();
			}
			
			TagDefinition tagDef = 
					new TagDefinition(
							null,
							tagDefinitionElement.getID(), 
							description,
							new Version(tagDefinitionElement.getAttributeValue(Attribute.n)), 
							null,
							tagDefinitionElement.getAttributeValue(Attribute.fsDecl_baseTypes));
			
			tagManager.addTagDefinition(tagsetDefinition, tagDef);
			
			addProperties(tagDef, 
					tagDefinitionElement.getChildNodes(
							TeiElementName.fDecl, 
							AttributeValue.f_Decl_name_catma_system_property.getStartsWithFilter()),
					tagDefinitionElement.getChildNodes(
							TeiElementName.fDecl, 
							AttributeValue.f_Decl_name_catma_system_property.getNotStartsWithFilter()));
		}
		
	}

	private void addProperties(
			TagDefinition tagDef, Nodes systemPropertyNodes, Nodes userPropertyNodes) {


		for (int i=0; i<systemPropertyNodes.size(); i++) {
			try {
				TeiElement sysPropElement = (TeiElement)systemPropertyNodes.get(i);
				PropertyDefinition pd = createPropertyDefinition(sysPropElement);
				tagDef.addSystemPropertyDefinition(pd);
			}
			catch(UnknownElementException uee) {
				ExceptionHandler.log(uee);
			}
				
		}
		
		for (int i=0; i<userPropertyNodes.size(); i++) {
			try {
				TeiElement userPropElement = (TeiElement)userPropertyNodes.get(i);
				PropertyDefinition pd = createPropertyDefinition(userPropElement);
				tagDef.addUserDefinedPropertyDefinition(pd);
			}
			catch(UnknownElementException uee) {
				ExceptionHandler.log(uee);
			}
				
		}
	}

	private PropertyDefinition createPropertyDefinition(
			TeiElement propElement) throws UnknownElementException {
		
		TeiElement valueElement = (TeiElement)propElement.getChildElements().get(0);
		
		PropertyValueFactory pvf = null;
		
		if (valueElement.is(TeiElementName.vRange)) {
			pvf = new ValueRangePropertyValueFactory(propElement);
		}
		else {
			throw new UnknownElementException(valueElement.getLocalName() + " is not supported!");
		}
		
		return new PropertyDefinition(
					propElement.getAttributeValue(Attribute.fDecl_name),
					pvf.getValueAsList());
	}

	public TagLibrary getTagLibrary() {
		return tagLibrary;
	}
}

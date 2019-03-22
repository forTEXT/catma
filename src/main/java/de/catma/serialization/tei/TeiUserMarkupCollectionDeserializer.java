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

import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import de.catma.ExceptionHandler;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.serialization.tei.PtrValueHandler.TargetValues;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.Version;
import de.catma.util.IDGenerator;
import nu.xom.Elements;
import nu.xom.Nodes;

public class TeiUserMarkupCollectionDeserializer {

	private TeiDocument teiDocument;
	private List<TagReference> tagReferences;
	private TagLibrary tagLibrary;
	private HashMap<String,String> old2newTagInstanceIDs = new HashMap<String, String>();

	public TeiUserMarkupCollectionDeserializer(
			TeiDocument teiDocument, TagLibrary tagLibrary, String collectionId) {
		this.teiDocument = teiDocument;
		this.tagLibrary = tagLibrary;
		this.tagReferences = new ArrayList<TagReference>();
		deserialize(collectionId);
	}

	private void deserialize(String collectionId) {
		Nodes segmentNodes = teiDocument.getNodes(
				TeiElementName.seg, AttributeValue.seg_ana_catma_tag_ref.getStartsWith());
		
		for (int i=0; i<segmentNodes.size(); i++) {
			TeiElement curSegment = (TeiElement)segmentNodes.get(i);
			AnaValueHandler anaValueHandler = new AnaValueHandler();
			List<String> tagInstanceIDs = 
					anaValueHandler.makeTagInstanceIDListFrom(
							curSegment.getAttributeValue(Attribute.ana));
			Elements pointerElements = curSegment.getChildElements(TeiElementName.ptr);
			for(String tagInstanceID : tagInstanceIDs) {
				TagInstance tagInstance = createTagInstance(tagInstanceID);
				
				for (int j=0; j<pointerElements.size();j++) {
					TeiElement curPointer = (TeiElement)pointerElements.get(j);
					PtrValueHandler ptrValueHandler = new PtrValueHandler();
					TargetValues targetValues =
							ptrValueHandler.getTargetValuesFrom(
									curPointer.getAttributeValue(Attribute.ptr_target));
					
					try {
						TagReference tagReference = 
							new TagReference(
									tagInstance, targetValues.getURI(), 
									targetValues.getRange(),
									collectionId);
						tagReferences.add(tagReference);
					}
					catch(URISyntaxException ue) {
						ExceptionHandler.log(ue);
					}
				}
			}
		}
		
	}
	
	public List<TagReference> getTagReferences() {
		return tagReferences;
	}

	private TagInstance createTagInstance(String tagInstanceID) {
		TeiElement tagInstanceElement = teiDocument.getElementByID(tagInstanceID);
		TagDefinition tagDefinition = tagLibrary.getTagDefinition(
				tagInstanceElement.getAttributeValue(Attribute.type));
		if (!old2newTagInstanceIDs.containsKey(tagInstanceElement.getID())) {
			old2newTagInstanceIDs.put(tagInstanceElement.getID(), new IDGenerator().generate());
		}
		final TagInstance tagInstance = 
			new TagInstance(
				old2newTagInstanceIDs.get(tagInstanceElement.getID()), 
				tagDefinition.getUuid(),
				tagDefinition.getAuthor(),
				ZonedDateTime.now().format(DateTimeFormatter.ofPattern(Version.DATETIMEPATTERN)),
				tagDefinition.getUserDefinedPropertyDefinitions(),
				tagDefinition.getTagsetDefinitionUuid());
		
		Nodes systemPropertyElements = tagInstanceElement.getChildNodes(
				TeiElementName.f,
				AttributeValue.f_name_catma_system_property.getStartsWithFilter());
		addProperties(
				tagDefinition, 
				new AddPropertyHandler() {
					public void addProperty(Property property) {
						tagInstance.addSystemProperty(property);
					}
				}, 
				systemPropertyElements);
		Nodes userDefinedPropertyElements = tagInstanceElement.getChildNodes(
				TeiElementName.f,
				AttributeValue.f_name_catma_system_property.getNotStartsWithFilter());
		addProperties(
				tagDefinition, 
				new AddPropertyHandler() {
			
					public void addProperty(Property property) {
						tagInstance.addUserDefinedProperty(property);
						
					}
				}, 
				userDefinedPropertyElements);
		return tagInstance;
	}

	private void addProperties(
			TagDefinition tagDefinition, 
			AddPropertyHandler addPropertyHandler, Nodes propertyElements) {
		
		for (int i=0; i<propertyElements.size(); i++) {
			try {
				TeiElement curPropertyElement = (TeiElement)propertyElements.get(i);
				//TODO: check if we need to make a new TeiDoc Version to support this, f_name no longer contains the name but the uuid!
				PropertyDefinition propertyDefinition =
						tagDefinition.getPropertyDefinition(
								curPropertyElement.getAttributeValue(Attribute.f_name));
				
				
				if (curPropertyElement.getChildElements().size() == 0) {
					addPropertyHandler.addProperty(
							new Property(
								propertyDefinition.getUuid(),
								Collections.emptyList()));
				}
				else {
					TeiElement valueElement = 
							(TeiElement)curPropertyElement.getChildElements().get(0);
					
					if (valueElement.is(TeiElementName.numeric)) {
						addPropertyHandler.addProperty(
							new Property(
								propertyDefinition.getUuid(),
									new NumericPropertyValueFactory(
										curPropertyElement).getValueAsList()));
					}
					else if (valueElement.is(TeiElementName.string)) {
						StringPropertyValueFactory stringPropFact = 
								new StringPropertyValueFactory(
										curPropertyElement);
						if (!stringPropFact.getValue().trim().isEmpty()) {
							addPropertyHandler.addProperty(
								new Property(
									propertyDefinition.getUuid(),
										stringPropFact.getValueAsList()));
						}
					}
					else if (valueElement.is(TeiElementName.vRange)) {
						TeiElement vColl = (TeiElement)valueElement.getChildElements().get(0);
						if (vColl.hasChildElements()) {
							List<String> valueList = new ArrayList<String>();
							
							for (int j=0; j<vColl.getChildElements().size(); j++) {
								valueList.add(new StringPropertyValueFactory(
													vColl, j).getValue());
							}
							
							addPropertyHandler.addProperty(new Property(
									propertyDefinition.getUuid(), 
									valueList));
						}
					}
					else {
						throw new UnknownElementException(
								valueElement.getLocalName() + " is not supported!");
					}
				}
			}
			catch(UnknownElementException ue) {
				ExceptionHandler.log(ue);
			}
		}
		
	}
	
	private static interface AddPropertyHandler {
		public void addProperty(Property property);
	}
}

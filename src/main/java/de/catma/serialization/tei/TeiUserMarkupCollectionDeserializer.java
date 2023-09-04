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

import de.catma.document.annotation.TagReference;
import de.catma.serialization.tei.PtrValueHandler.TargetValues;
import de.catma.tag.*;
import de.catma.util.IDGenerator;
import nu.xom.Elements;
import nu.xom.Nodes;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TeiUserMarkupCollectionDeserializer {
	private final Logger logger = Logger.getLogger(TeiUserMarkupCollectionDeserializer.class.getName());

	private final TeiDocument teiDocument;
	private final TagLibrary tagLibrary;

	private final List<TagReference> tagReferences;
	private final HashMap<String,String> oldToNewTagInstanceIds;
	private final IDGenerator idGenerator;

	public TeiUserMarkupCollectionDeserializer(TeiDocument teiDocument, TagLibrary tagLibrary, String collectionId) {
		this.teiDocument = teiDocument;
		this.tagLibrary = tagLibrary;

		this.tagReferences = new ArrayList<>();
		this.oldToNewTagInstanceIds = new HashMap<>();
		this.idGenerator = new IDGenerator();

		deserialize(collectionId);
	}

	public List<TagReference> getTagReferences() {
		return tagReferences;
	}

	private void deserialize(String collectionId) {
		Nodes segmentNodes = teiDocument.getNodes(TeiElementName.seg, AttributeValue.seg_ana_catma_tag_ref.getStartsWith());

		// TODO: make these static
		AnaValueHandler anaValueHandler = new AnaValueHandler();
		PtrValueHandler ptrValueHandler = new PtrValueHandler();

		for (int i=0; i<segmentNodes.size(); i++) {
			TeiElement currentSegment = (TeiElement) segmentNodes.get(i);

			List<String> tagInstanceIds = anaValueHandler.makeTagInstanceIDListFrom(currentSegment.getAttributeValue(Attribute.ana));

			Elements pointerElements = currentSegment.getChildElements(TeiElementName.ptr);
			List<TargetValues> pointerElementsTargetValues = new ArrayList<>();
			for (int j=0; j<pointerElements.size(); j++) {
				TeiElement currentPointer = (TeiElement) pointerElements.get(j);
				pointerElementsTargetValues.add(
						ptrValueHandler.getTargetValuesFrom(currentPointer.getAttributeValue(Attribute.ptr_target))
				);
			}

			for (String tagInstanceId : tagInstanceIds) {
				TagInstance tagInstance = createTagInstance(tagInstanceId);
				pointerElementsTargetValues.forEach(targetValues -> tagReferences.add(
						new TagReference(collectionId, tagInstance, targetValues.getURI(), targetValues.getRange())
				));
			}
		}
	}

	private TagInstance createTagInstance(String tagInstanceId) {
		TeiElement tagInstanceElement = teiDocument.getElementByID(tagInstanceId);
		TagDefinition tagDefinition = tagLibrary.getTagDefinition(tagInstanceElement.getAttributeValue(Attribute.type));

		if (!oldToNewTagInstanceIds.containsKey(tagInstanceElement.getID())) {
			oldToNewTagInstanceIds.put(tagInstanceElement.getID(), idGenerator.generate());
		}

		final TagInstance tagInstance = new TagInstance(
				oldToNewTagInstanceIds.get(tagInstanceElement.getID()),
				tagDefinition.getUuid(),
				tagDefinition.getAuthor(),
				ZonedDateTime.now().format(DateTimeFormatter.ofPattern(Version.DATETIMEPATTERN)),
				tagDefinition.getUserDefinedPropertyDefinitions(),
				tagDefinition.getTagsetDefinitionUuid()
		);

		Nodes systemPropertyElements = tagInstanceElement.getChildNodes(
				TeiElementName.f,
				AttributeValue.f_name_catma_system_property.getStartsWithFilter()
		);
		addProperties(
				tagDefinition,
				new AddPropertyHandler() {
					public void addProperty(Property property) {
						tagInstance.addSystemProperty(property);
					}
				},
				systemPropertyElements
		);

		Nodes userDefinedPropertyElements = tagInstanceElement.getChildNodes(
				TeiElementName.f,
				AttributeValue.f_name_catma_system_property.getNotStartsWithFilter()
		);
		addProperties(
				tagDefinition, 
				new AddPropertyHandler() {
					public void addProperty(Property property) {
						tagInstance.addUserDefinedProperty(property);
					}
				}, 
				userDefinedPropertyElements
		);

		return tagInstance;
	}

	private void addProperties(TagDefinition tagDefinition, AddPropertyHandler addPropertyHandler, Nodes propertyElements) {
		for (int i=0; i<propertyElements.size(); i++) {
			try {
				TeiElement currentPropertyElement = (TeiElement) propertyElements.get(i);

				PropertyDefinition propertyDefinition = tagDefinition.getPropertyDefinition(
						currentPropertyElement.getAttributeValue(Attribute.f_name)
				);

				if (currentPropertyElement.getChildElements().size() == 0) {
					addPropertyHandler.addProperty(
							new Property(propertyDefinition.getUuid(), Collections.emptyList())
					);
					return;
				}

				TeiElement valueElement = (TeiElement) currentPropertyElement.getChildElements().get(0);

				if (valueElement.is(TeiElementName.numeric)) {
					addPropertyHandler.addProperty(
							new Property(propertyDefinition.getUuid(), new NumericPropertyValueFactory(currentPropertyElement).getValueAsList())
					);
				}
				else if (valueElement.is(TeiElementName.string)) {
					StringPropertyValueFactory stringPropertyValueFactory = new StringPropertyValueFactory(currentPropertyElement);
					if (!stringPropertyValueFactory.getValue().trim().isEmpty()) {
						addPropertyHandler.addProperty(
								new Property(propertyDefinition.getUuid(), stringPropertyValueFactory.getValueAsList())
						);
					}
				}
				else if (valueElement.is(TeiElementName.vRange)) {
					TeiElement vColl = (TeiElement) valueElement.getChildElements().get(0);
					if (vColl.hasChildElements()) {
						List<String> values = new ArrayList<>();

						for (int j=0; j<vColl.getChildElements().size(); j++) {
							values.add(new StringPropertyValueFactory(vColl, j).getValue());
						}

						addPropertyHandler.addProperty(
								new Property(propertyDefinition.getUuid(), values)
						);
					}
				}
				else {
					throw new UnknownElementException(
							String.format("%s is not supported", valueElement.getLocalName())
					);
				}
			}
			catch (UnknownElementException uee) {
				logger.log(Level.SEVERE, "Error adding properties", uee);
			}
		}
	}

	private interface AddPropertyHandler {
		void addProperty(Property property);
	}
}

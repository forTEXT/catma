package de.catma.serialization.tei;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import nu.xom.Elements;
import nu.xom.Nodes;
import de.catma.core.ExceptionHandler;
import de.catma.core.document.standoffmarkup.usermarkup.TagReference;
import de.catma.core.tag.Property;
import de.catma.core.tag.PropertyDefinition;
import de.catma.core.tag.PropertyValueList;
import de.catma.core.tag.TagDefinition;
import de.catma.core.tag.TagInstance;
import de.catma.core.tag.ITagLibrary;
import de.catma.serialization.tei.PtrValueHandler.TargetValues;

public class TeiUserMarkupCollectionDeserializer {

	private TeiDocument teiDocument;
	private List<TagReference> tagReferences;
	private ITagLibrary tagLibrary;

	public TeiUserMarkupCollectionDeserializer(
			TeiDocument teiDocument, ITagLibrary tagLibrary) {
		this.teiDocument = teiDocument;
		this.tagLibrary = tagLibrary;
		this.tagReferences = new ArrayList<TagReference>();
		deserialize();
	}

	private void deserialize() {
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
									tagInstance, targetValues.getURI(), targetValues.getRange());
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
		final TagInstance tagInstance = 
				new TagInstance(tagInstanceElement.getID(), tagDefinition);
		
		Nodes systemPropertyElements = tagInstanceElement.getChildNodes(
				TeiElementName.f,
				AttributeValue.f_name_catma_system_property.getStartsWithFilter());
		addProperties(
				tagInstance.getTagDefinition(), 
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
				tagInstance.getTagDefinition(), 
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
				TeiElement curSystemPropertyElement = (TeiElement)propertyElements.get(i);
				PropertyDefinition propertyDefinition =
						tagDefinition.getPropertyDefinitionByName(
								curSystemPropertyElement.getAttributeValue(Attribute.f_name));
				TeiElement valueElement = 
						(TeiElement)curSystemPropertyElement.getChildElements().get(0);
				
				if (valueElement.is(TeiElementName.numeric)) {
					addPropertyHandler.addProperty(
						new Property(
							propertyDefinition,
							new PropertyValueList(
									new NumericPropertyValueFactory(
											curSystemPropertyElement).getValueAsList())));
				}
				else if (valueElement.is(TeiElementName.string)) {
					addPropertyHandler.addProperty(
							new Property(
								propertyDefinition,
								new PropertyValueList(
										new StringPropertyValueFactory(
												curSystemPropertyElement).getValueAsList())));				
				}
				else {
					throw new UnknownElementException(
							valueElement.getLocalName() + " is not supported!");
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

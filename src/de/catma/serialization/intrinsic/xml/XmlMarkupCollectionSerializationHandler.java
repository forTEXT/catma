package de.catma.serialization.intrinsic.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;
import de.catma.document.Range;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.contenthandler.XMLContentHandler;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.serialization.UserMarkupCollectionSerializationHandler;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyPossibleValueList;
import de.catma.tag.PropertyValueList;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.util.ColorConverter;
import de.catma.util.IDGenerator;

public class XmlMarkupCollectionSerializationHandler implements
		UserMarkupCollectionSerializationHandler {
	
	private TagManager tagManager;
	private String sourceDocumentId;
	private IDGenerator idGenerator;
	private HashMap<String, TagDefinition> pathToTagDefMap;
	private XMLContentHandler xmlContentHandler;
	
	public XmlMarkupCollectionSerializationHandler(
			TagManager tagManager, String sourceDocumentId, XMLContentHandler xmlContentHandler) {
		super();
		this.tagManager = tagManager;
		this.sourceDocumentId = sourceDocumentId;
		this.idGenerator = new IDGenerator();
		this.pathToTagDefMap = new HashMap<>();
		this.xmlContentHandler = xmlContentHandler;
	}

	@Override
	public void serialize(UserMarkupCollection userMarkupCollection,
			SourceDocument sourceDocument, OutputStream outputStream)
			throws IOException {
		throw new UnsupportedOperationException(
			"serialization of xml intrinsic markup collections to their "
			+ "original format is not supported yet!");
	}

	@Override
	public UserMarkupCollection deserialize(String id, InputStream inputStream)
			throws IOException {
		
		try {
	        Builder builder = new Builder();
	        Document document = builder.build(inputStream);
	        StringBuilder contentBuilder = new StringBuilder();
	        TagLibrary tagLibrary = new TagLibrary(null, "Intrinsic Markup");
	        TagsetDefinition tagsetDefinition = 
	        		new TagsetDefinition(
	        			null, idGenerator.generate(), 
	        			"Intrinsic Markup", new Version());
	        
	        tagManager.addTagsetDefinition(tagLibrary, tagsetDefinition);
	        Stack<String> elementStack = new Stack<String>();
	        UserMarkupCollection userMarkupCollection = 
	        	new UserMarkupCollection(
	        		null, 
	        		new ContentInfoSet(
	        			"", "Intrinsic Markup", "", "Intrinsic Markup"), 
	        		tagLibrary);
	        
	        scanElements(
	        		contentBuilder, 
	        		document.getRootElement(), 
	        		elementStack, tagManager,
	        		tagLibrary, tagsetDefinition,
	        		userMarkupCollection);
	        
	        return userMarkupCollection;
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

    private void scanElements(
    		StringBuilder contentBuilder, Element element,
    		Stack<String> elementStack, TagManager tagManager, 
    		TagLibrary tagLibrary, 
    		TagsetDefinition tagsetDefinition,
    		UserMarkupCollection userMarkupCollection) throws URISyntaxException {
    	
		int start = contentBuilder.length();

		StringBuilder pathBuilder = new StringBuilder();
        for (int j=0; j<elementStack.size(); j++){
        	pathBuilder.append("/" + elementStack.get(j));
        }
        
        String parentPath = pathBuilder.toString();
        
        elementStack.push(element.getQualifiedName()); 
        String path = parentPath + "/" + elementStack.peek();

        TagDefinition tagDefinition = pathToTagDefMap.get(path);
        
        if (tagDefinition == null) {
        	TagDefinition parentTag = pathToTagDefMap.get(parentPath);
        	String parentUuid = (parentTag==null)?null:parentTag.getUuid();
        	tagDefinition = 
        		new TagDefinition(
        			null, idGenerator.generate(), 
        			elementStack.peek(), new Version(), null, parentUuid);
        	PropertyDefinition colorDef = 
        		new PropertyDefinition(
        			null, 
        			idGenerator.generate(), 
        			PropertyDefinition.SystemPropertyName.catma_displaycolor.name(), 
        			new PropertyPossibleValueList(
        				ColorConverter.toRGBIntAsString(ColorConverter.randomHex())));
        	tagDefinition.addSystemPropertyDefinition(colorDef);
        			
        	pathToTagDefMap.put(path, tagDefinition);
        	tagManager.addTagDefinition(tagsetDefinition, tagDefinition);
        }

		for( int idx=0; idx<element.getChildCount(); idx++) {
            Node curChild = element.getChild(idx);
            if (curChild instanceof Text) {
            	xmlContentHandler.addTextContent(contentBuilder, element, curChild.getValue());
            }
            else if (curChild instanceof Element) { //descent
                scanElements(
                	contentBuilder, 
                	(Element)curChild, 
                	elementStack,
                	tagManager,
                	tagLibrary,
                	tagsetDefinition,
                	userMarkupCollection);
            
            }
        }
		
		if (element.getChildCount() == 0) {
			xmlContentHandler.addEmptyElement(contentBuilder, element);
		}
        
        int end = contentBuilder.length();
        Range range = new Range(start,end);

        TagInstance tagInstance = new TagInstance(idGenerator.generate(), tagDefinition);

        for (int i=0; i<element.getAttributeCount(); i++) {
        	PropertyDefinition propertyDefinition = 
        		tagDefinition.getPropertyDefinitionByName(
        				element.getAttribute(i).getQualifiedName());
        	
        	if (propertyDefinition == null) {
        		propertyDefinition = 
        			new PropertyDefinition(
        				null, idGenerator.generate(), 
        				element.getAttribute(i).getQualifiedName(), 
        				new PropertyPossibleValueList(element.getAttribute(i).getValue()));
        		tagManager.addUserDefinedPropertyDefinition(tagDefinition, propertyDefinition);
        	}
        	else if (!propertyDefinition
        			.getPossibleValueList()
        			.getPropertyValueList()
        			.getValues().contains(element.getAttribute(i).getValue())) {
        		List<String> newValueList = new ArrayList<>();
        		newValueList.addAll(
        				propertyDefinition
        						.getPossibleValueList()
        						.getPropertyValueList()
        						.getValues());
        		newValueList.add(element.getAttribute(i).getValue());
        		propertyDefinition.setPossibleValueList(
        			new PropertyPossibleValueList(newValueList, false));
        		
        	}
        	Property property = 
        		new Property(
        			propertyDefinition, 
        			new PropertyValueList(element.getAttribute(i).getValue()));
        	tagInstance.addUserDefinedProperty(property);
        }
        														
        TagReference tagReference = new TagReference(tagInstance, sourceDocumentId, range);
        userMarkupCollection.addTagReference(tagReference);
     
        elementStack.pop();	
    }

}

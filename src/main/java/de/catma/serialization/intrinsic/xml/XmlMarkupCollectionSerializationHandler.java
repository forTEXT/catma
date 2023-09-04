package de.catma.serialization.intrinsic.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import de.catma.document.Range;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.contenthandler.XML2ContentHandler;
import de.catma.serialization.AnnotationCollectionSerializationHandler;
import de.catma.tag.KnownTagsetDefinitionName;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.util.ColorConverter;
import de.catma.util.IDGenerator;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;

public class XmlMarkupCollectionSerializationHandler implements
		AnnotationCollectionSerializationHandler {
	
	public final static String DEFAULT_COLLECTION_TITLE = "Intrinsic Markup";
	
	private TagManager tagManager;
	private IDGenerator idGenerator;
	private XML2ContentHandler xmlContentHandler;
	private String author;
	

	
	public XmlMarkupCollectionSerializationHandler(
			TagManager tagManager, XML2ContentHandler xmlContentHandler, String author) {
		super();
		this.tagManager = tagManager;
		this.idGenerator = new IDGenerator();
		this.xmlContentHandler = xmlContentHandler;
		this.author = author;
	}

	@Override
	public void serialize(AnnotationCollection userMarkupCollection,
			SourceDocument sourceDocument, OutputStream outputStream)
			throws IOException {
		throw new UnsupportedOperationException(
			"Serialization of XML intrinsic annotation collections to their "
			+ "original format is not supported yet");
	}

	@Override
	public AnnotationCollection deserialize(SourceDocument sourceDocument, String id, InputStream inputStream)
			throws IOException {
		
		try {
	        Builder builder = new Builder();
	        Document document = builder.build(inputStream);
	        Map<String, String> namespacePrefixToTagsetIdMap = new HashMap<>();
	        for (int idx=0; idx<document.getRootElement().getNamespaceDeclarationCount(); idx++) {
	        	String prefix = document.getRootElement().getNamespacePrefix(idx);
	        	String namespaceURI = document.getRootElement().getNamespaceURI(prefix);
	        	if (namespaceURI != null && !namespaceURI.isEmpty()) {
		        	String tagsetId = idGenerator.generateTagsetId(namespaceURI);
	
		        	if (tagManager.getTagLibrary().getTagsetDefinition(tagsetId) == null) {
		        		TagsetDefinition tagsetDefinition = 
		        				new TagsetDefinition(
		        						tagsetId, 
		        						namespaceURI);
		        		tagsetDefinition.setResponsibleUser(author);
		        		
		        		tagManager.addTagsetDefinition(tagsetDefinition);
		        	}
		        	namespacePrefixToTagsetIdMap.put(prefix, tagsetId);
	        	}
	        }
	        
	        String defaultIntrinsicXmlTagsetId = 
	        		KnownTagsetDefinitionName.DEFAULT_INTRINSIC_XML.asTagsetId();
	        
	        StringBuilder contentBuilder = new StringBuilder();
	        
	        if (tagManager.getTagLibrary().getTagsetDefinition(defaultIntrinsicXmlTagsetId) == null) {
	        	TagsetDefinition tagsetDefinition = 
	        			new TagsetDefinition(
	        					defaultIntrinsicXmlTagsetId, 
	        					null);
	        	
	        	tagManager.addTagsetDefinition(tagsetDefinition);
	        }
	        
	        
	        Stack<String> elementStack = new Stack<String>();
	        AnnotationCollection userMarkupCollection = 
	        	new AnnotationCollection(
	        		id,
	        		new ContentInfoSet(
	        			"", 
	        			sourceDocument.toString() + " Intrinsic Markup", 
	        			"", 
	        			DEFAULT_COLLECTION_TITLE), 
	        		tagManager.getTagLibrary(),
	        		sourceDocument.getUuid(),
	        		null,
	        		null);
	        
	        scanElements(
	        		contentBuilder, 
	        		document.getRootElement(), 
	        		elementStack, tagManager,
	        		tagManager.getTagLibrary(),
	        		namespacePrefixToTagsetIdMap,
	        		userMarkupCollection,
	        		sourceDocument.getUuid(),
	        		sourceDocument.getLength());
	        
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
    		Map<String, String> namespacePrefixToTagsetIdMap,
    		AnnotationCollection userMarkupCollection,
    		String docId,
    		int docLength) throws Exception {
    	
		int start = contentBuilder.length();

		StringBuilder pathBuilder = new StringBuilder();
        for (int j=0; j<elementStack.size(); j++){
        	pathBuilder.append("/" + elementStack.get(j));
        }
        
        String parentPath = pathBuilder.toString();
        
        elementStack.push(element.getLocalName()); 
        String path = parentPath + "/" + elementStack.peek();

        String tagName = element.getLocalName();

        String prefix = element.getNamespacePrefix();
        String tagsetId = namespacePrefixToTagsetIdMap.get(prefix);
        if (tagsetId == null) {
	        tagsetId = KnownTagsetDefinitionName.DEFAULT_INTRINSIC_XML.asTagsetId();
        }

        TagsetDefinition tagset = tagLibrary.getTagsetDefinition(tagsetId);
        String tagId = idGenerator.generate();
        
        
        TagDefinition tagDefinition = tagset.getTagDefinitionsByName(tagName).findFirst().orElse(null);

        String pathPropertyDefId = null;
        
        if (tagDefinition == null) {
        	
        	tagDefinition = 
        		new TagDefinition(
        			tagId, 
        			elementStack.peek(),
        			null, //no parent, hierarchy is collected in annotation property
        			tagsetId);
    		tagDefinition.addSystemPropertyDefinition(
    				new PropertyDefinition(
    					idGenerator.generate(PropertyDefinition.SystemPropertyName.catma_displaycolor.name()), 
    					PropertyDefinition.SystemPropertyName.catma_displaycolor.name(), 
    					Collections.singletonList(ColorConverter.toRGBIntAsString(ColorConverter.randomHex()))));
    		tagDefinition.addSystemPropertyDefinition(
    				new PropertyDefinition(
    					idGenerator.generate(PropertyDefinition.SystemPropertyName.catma_markupauthor.name()), 
    					PropertyDefinition.SystemPropertyName.catma_markupauthor.name(), 
    					Collections.singletonList(author)));
    		
    		pathPropertyDefId = idGenerator.generate();
    		
        	PropertyDefinition pathDef = 
        		new PropertyDefinition(
        				pathPropertyDefId,
            			"path", 
            			Collections.emptyList());
        	tagDefinition.addUserDefinedPropertyDefinition(pathDef);
        	
        	tagManager.addTagDefinition(tagset, tagDefinition);
        }
        else {
        	pathPropertyDefId = tagDefinition.getPropertyDefinition("path").getUuid();
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
                	namespacePrefixToTagsetIdMap,
                	userMarkupCollection,
                	docId,
                	docLength);
            
            }
        }
		
		if (element.getChildCount() != 0) {
			xmlContentHandler.addBreak(contentBuilder, element);
		}
		
		
		int end = contentBuilder.length();
		Range range = new Range(start,end);
		  
        
        if (range.isSinglePoint()) {

        	int newStart = range.getStartPoint();
			if (newStart > 0) {
				newStart = newStart-1;
			}
			
			int newEnd = range.getEndPoint();
			if (newEnd < docLength-1) {
				newEnd = newEnd+1;
			}
			
			range = new Range(newStart, newEnd);
			
        }
        
        TagInstance tagInstance = new TagInstance(
        	idGenerator.generate(), 
        	tagDefinition.getUuid(),
        	author,
        	ZonedDateTime.now().format(DateTimeFormatter.ofPattern(Version.DATETIMEPATTERN)),
        	tagDefinition.getUserDefinedPropertyDefinitions(),
        	tagDefinition.getTagsetDefinitionUuid());


        for (int i=0; i<element.getAttributeCount(); i++) {
        	PropertyDefinition propertyDefinition = 
        		tagDefinition.getPropertyDefinition(
        				element.getAttribute(i).getQualifiedName());
        	
        	if (propertyDefinition == null) {
        		propertyDefinition = 
        			new PropertyDefinition(
        				idGenerator.generate(),
        				element.getAttribute(i).getQualifiedName(), 
        				Collections.singleton(element.getAttribute(i).getValue()));
        		tagManager.addUserDefinedPropertyDefinition(tagDefinition, propertyDefinition);
        	}
        	else if (!propertyDefinition
        			.getPossibleValueList()
        			.contains(element.getAttribute(i).getValue())) {
        		List<String> newValueList = new ArrayList<>();
        		newValueList.addAll(propertyDefinition.getPossibleValueList());
        		newValueList.add(element.getAttribute(i).getValue());
        		propertyDefinition.setPossibleValueList(newValueList);
        		
        	}
        	Property property = 
        		new Property(
        			propertyDefinition.getUuid(), 
        			Collections.singleton(element.getAttribute(i).getValue()));
        	tagInstance.addUserDefinedProperty(property);
        }
        
        Property pathProperty = 
        	new Property(pathPropertyDefId, Collections.singletonList(path));
        tagInstance.addUserDefinedProperty(pathProperty);
        	
        														
        TagReference tagReference = new TagReference(
                userMarkupCollection.getId(), tagInstance, docId, range);
        userMarkupCollection.addTagReference(tagReference);
     
        elementStack.pop();	
    }

}

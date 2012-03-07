package de.catma.serialization.tei;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Nodes;
import de.catma.core.util.IDGenerator;


public class V3TeiDocumentConverter implements TeiDocumentConverter {
	
	private static final class TagDef {
		private String color;
		private HashMap<String, String> properties;
		public TagDef(String color) {
			this.properties = new HashMap<String, String>();
			this.color = color;
		}
	}
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd'T'HH:mm:ssZ");
	private TeiElement standardTagsetDefinition;
	private HashMap<String,TeiElement> tagsetDefinitions = 
			new HashMap<String, TeiElement>();
	private HashMap<String,TagDef> tagDefinitions = 
			new HashMap<String,TagDef>();
	
	public void convert(TeiDocument teiDocument) {
	
		TeiElement encodingDesc = new TeiElement(TeiElementName.encodingDesc);
		((TeiElement)teiDocument.getNodes(TeiElementName.teiHeader).get(0)).appendChild(encodingDesc);
		

		Nodes tagElements = 
				teiDocument.getNodes(TeiElementName.fs, AttributeValue.type_catma_tag);
		
		if (tagElements.size() != 0) {
			standardTagsetDefinition = addStandardTagset(encodingDesc);
		}
		
		Nodes tagsetElements = teiDocument.getNodes(TeiElementName.fvLib);
		
		for( int i=0; i<tagsetElements.size(); i++) {
			TeiElement tagsetElement = (TeiElement)tagsetElements.get(i);
			String version = tagsetElement.getAttributeValue(Attribute.n);
			version = version.substring(version.indexOf(' ')+1);
			
			// skip deleted or corrupted taglibs
			if (!version.startsWith("-") && tagsetElement.hasChildElements()) { 
				TeiElement tagsetDef = createTagsetDefinition(tagsetElement, encodingDesc);
				tagsetDefinitions.put(tagsetDef.getID(), tagsetDef);
			}
		}	
		
		
		for( int i=0; i<tagElements.size(); i++) {
			createTagDefinition(tagElements.get(i));
		}
		
		for( int i=0; i<tagsetElements.size(); i++) {
			TeiElement tagsetElement = (TeiElement)tagsetElements.get(i);
		
			if (tagsetElement.getParent() != null) {
				tagsetElement.getParent().removeChild(tagsetElement);
			}
		}
		
		for( int i=0; i<tagElements.size(); i++) {
			TeiElement tagElement = (TeiElement)tagElements.get(i);
			if (tagElement.getParent() != null) {
				tagElement.getParent().removeChild(tagElement);
			}
		}
		
		IDGenerator catmaIDGenerator = new IDGenerator();
		Nodes segElements = teiDocument.getNodes(TeiElementName.seg);
		TeiElement text = (TeiElement)teiDocument.getNodes(TeiElementName.text).get(0);
		for (int i=0; i<segElements.size(); i++) {
			TeiElement segElement = (TeiElement)segElements.get(i);
			if ((segElement.getAttributeValue(Attribute.ana) != null) 
					&& !segElement.getAttributeValue(Attribute.ana).isEmpty()) {
				addTagInstance(segElement, catmaIDGenerator, text);
			}
		}
		
		adjustPointers(teiDocument);
		
		teiDocument.getTeiHeader().getTechnicalDescription().setVersion(TeiDocumentVersion.V3);
		
//		teiDocument.printXmlDocument();
	}

	private void adjustPointers(TeiDocument teiDocument) {
		Nodes pointers = teiDocument.getNodes(TeiElementName.ptr);
		
		for( int i=0; i<pointers.size(); i++) {
			TeiElement pointer = (TeiElement)pointers.get(i);
			String target = pointer.getAttributeValue(Attribute.ptr_target);
			
			String[] uri_points = target.split( "#" );
			String uri = uri_points[0].trim();
			String[] points = uri_points[1].split( "/." );
			
			String newTarget = 
					"catma:///" + uri + "#char=" 
							+ points[1].substring( 0, points[1].indexOf( ',' ) ).trim()
							+ ","
							+ points[2].substring( 0, points[2].indexOf( ')' ) ).trim();
			pointer.setAttributeValue(Attribute.ptr_target, newTarget);
		}
		
	}

	private void addTagInstance(TeiElement segElement, IDGenerator catmaIDGenerator, TeiElement text) {
		String references = segElement.getAttributeValue(Attribute.ana);
		String[] idValues = references.trim().split( "#" );
		StringBuilder newReferencesBuilder = new StringBuilder();
		
		for( String id : idValues ) {
			if (!id.trim().isEmpty()) {
				String instanceID = catmaIDGenerator.generate();
				TagDef tagDefinition = tagDefinitions.get(id.trim());
				newReferencesBuilder.append(" #");
				newReferencesBuilder.append(instanceID);
				
				TeiElement fs = new TeiElement(TeiElementName.fs);
				
				fs.setID(instanceID);
				fs.setAttributeValue(Attribute.type, id.trim());
				
				TeiElement fColor = new TeiElement(TeiElementName.f);
				fColor.setAttributeValue(Attribute.f_name, "catma_displaycolor");
				TeiElement numeric = new TeiElement(TeiElementName.numeric);
				numeric.setAttributeValue(Attribute.numeric_value, tagDefinition.color);
				fColor.appendChild(numeric);
				fs.appendChild(fColor);
				
				for(Map.Entry<String, String> entry : tagDefinition.properties.entrySet()) {			
					TeiElement f = new TeiElement(TeiElementName.f);
					f.setAttributeValue(Attribute.f_name, entry.getKey());
					TeiElement string = new TeiElement(TeiElementName.string);
					string.appendChild(entry.getValue());
					f.appendChild(string);
					fs.appendChild(f);
				}
				
				text.insertChild(fs,0);
			}
		}
		segElement.setAttributeValue(Attribute.ana, newReferencesBuilder.toString().trim());
	}

	private void createTagDefinition(Node node) {
		TeiElement tagElement = (TeiElement)node;
		String version = tagElement.getAttributeValue(Attribute.n);
		
		if (!version.startsWith("-") && !tagDefinitions.containsKey(tagElement.getID())) {
			Elements properties = tagElement.getChildElements(TeiElementName.f);
			TeiElement tagsetDef = getTagsetDef(tagElement);
			if (tagsetDef != null) { // check for deleted tagsets
				String baseTagID = getBaseTagID(tagElement);
	
				TeiElement tagDefinition = addTagdefinition(
						tagElement.getID(), 
						getTimestamp(),
						getTagName(properties),
						getColorValue(properties),
						baseTagID,
						false,
						tagsetDef);
				
				addUserDefinedProperties(tagDefinition, properties);
			}
		}
		
	}

	private void addUserDefinedProperties(TeiElement tagDefinition, Elements properties) {
		for(int i=0; i<properties.size();i++) {
			TeiElement curProperty = (TeiElement)properties.get(i);
			
			if (!curProperty.hasChildElements()) { // hack to cover empty f-elements from corrupted TEI-docs
				TeiElement string = new TeiElement(TeiElementName.string);
				string.appendChild("property recovered recovered from corrupt TEI file");
				curProperty.appendChild(string);
			}
			
			String curPropertyName = curProperty.getAttributeValue(Attribute.f_name);
			
			if (!curPropertyName.equals("catma_displaycolor")
					&& !curPropertyName.equals("catma_tagname")
					&& !((TeiElement)curProperty.getChildElements().get(0)).is(TeiElementName.fs)) {
				String curPropertyValue = curProperty.getChildElements().get(0).getValue();
				
				addUserDefinedProperty(tagDefinition, curPropertyName, curPropertyValue);
				tagDefinitions.get(tagDefinition.getID()).properties.put(curPropertyName, curPropertyValue);
			}
		}

	}

	private void addUserDefinedProperty(TeiElement tagDefinition, String propertyName, String propertyValue) {
		TeiElement fDecl = new TeiElement(TeiElementName.fDecl);
		fDecl.setAttributeValue(Attribute.fDecl_name, propertyName);
		fDecl.setAttributeValue(Attribute.fDecl_optional, "false");
		TeiElement vRange = new TeiElement(TeiElementName.vRange);
		TeiElement string = new TeiElement(TeiElementName.string);
		string.appendChild(propertyValue);
		vRange.appendChild(string);
		fDecl.appendChild(vRange);
		tagDefinition.appendChild(fDecl);
	}

	private String getBaseTagID(TeiElement tagElement) {
		TeiElement parent = tagElement.getTeiElementParent();
		
		if (parent == null) {
			throw new IllegalStateException(
					"could not find tagset definition for " + tagElement.getLocalName());
		}	
		
		if (parent.is(TeiElementName.text)||parent.is(TeiElementName.fvLib)) {
			return "CATMA_BASE_TAG";
		}
		else if ((parent.is(TeiElementName.f) 
				&& parent.getTeiElementParent().is(TeiElementName.fs))) {
			
			return parent.getTeiElementParent().getID();
			
		}
		else {
			throw new IllegalStateException("unexpected parent element: " + parent);
		}

	}

	private TeiElement getTagsetDef(TeiElement tagElement) {
		TeiElement parent = tagElement.getTeiElementParent();
		
		if (parent == null) {
			throw new IllegalStateException(
					"could not find tagset definition for " + tagElement.getLocalName());
		}
		
		if (parent.is(TeiElementName.text)) {
			return standardTagsetDefinition;
		}
		else if (parent.is(TeiElementName.fvLib)) {
			return tagsetDefinitions.get(parent.getID());
		}
		else {
			return getTagsetDef(parent);
		}
	}

	private String getTagName(Elements properties) {
		for(int i=0; i<properties.size();i++) {
			TeiElement curProperty = (TeiElement)properties.get(i);
			if (curProperty.getAttributeValue(Attribute.f_name).equals("catma_tagname")) {
				return curProperty.getChildElements().get(0).getValue();
			}
		}
		throw new IllegalStateException("catma_tagname could not be found!");
	}

	private String getColorValue(Elements properties) {
		for(int i=0; i<properties.size();i++) {
			TeiElement curProperty = (TeiElement)properties.get(i);
			if (curProperty.getAttributeValue(Attribute.f_name).equals("catma_displaycolor")) {
				return ((TeiElement)curProperty.getChildElements().get(0)).getAttributeValue(
						Attribute.numeric_value);
			}
		}
		throw new IllegalStateException("catma_displaycolor could not be found!");
	}

	private TeiElement addTagdefinition(
			String id, String nValue, String tagName, String colorValue, 
			String baseTagID, boolean forBase,
			TeiElement tagsetDefinition) {
		
		TeiElement fsDecl = new TeiElement(TeiElementName.fsDecl);
		fsDecl.setID(id);
		fsDecl.setAttributeValue(Attribute.n, nValue);
		fsDecl.setAttributeValue(Attribute.type, id);
		if (!forBase) {
			fsDecl.setAttributeValue(Attribute.fsDecl_baseTypes, baseTagID);
		}
		
		TeiElement fsDescr = new TeiElement(TeiElementName.fsDescr);
		fsDescr.appendChild(tagName);
		
		TeiElement fDecl = new TeiElement(TeiElementName.fDecl);
		fDecl.setAttributeValue(Attribute.fDecl_name, "catma_displaycolor");

		if (forBase) {
			fDecl.setAttributeValue(Attribute.fDecl_optional, "false");
			
		}
		
		TeiElement vRange = new TeiElement(TeiElementName.vRange);
		
		TeiElement numeric = new TeiElement(TeiElementName.numeric);
		numeric.setAttributeValue(Attribute.numeric_value, colorValue);
		if (forBase){
			numeric.setAttributeValue(Attribute.numeric_max, "-1");
		}
		vRange.appendChild(numeric);
		fDecl.appendChild(vRange);
		fsDecl.appendChild(fsDescr);
		fsDecl.appendChild(fDecl);
		tagsetDefinition.appendChild(fsDecl);
		tagDefinitions.put(id, new TagDef(colorValue));
		
		return fsDecl;
	}

	private TeiElement createTagsetDefinition(Node node, TeiElement encodingDesc) {
		TeiElement tagsetElement = (TeiElement)node;
		String tagsetID = tagsetElement.getID();
		String nValue = tagsetElement.getAttributeValue(Attribute.n);
		String tagsetName = nValue.substring(0, nValue.lastIndexOf(' ')); 
		return addTagsetDefinition(tagsetID, tagsetName+" "+getTimestamp(), encodingDesc);
	}

	private TeiElement addStandardTagset(TeiElement encodingDesc) {
		/* 
		 	<fsdDecl xml:id="CATMA_STANDARD_TAGSET" n="Standard Tagset 1">
				<!-- base tag -->
				<fsDecl xml:id="CATMA_BASE_TAG" n="1" type="CATMA_BASE_TAG">
					<fsDescr>CATMA Base Tag</fsDescr>
					<fDecl name="catma_displaycolor"
						optional="false">
						<vRange>
							<numeric value="-16777216" max="-1"/>
						</vRange>
					</fDecl>
				</fsDecl>
			</fsdDecl>
		 */
		
		TeiElement fsdDecl = 
				addTagsetDefinition(
						"CATMA_STANDARD_TAGSET", "Standard Tagset 1", encodingDesc );
		addTagdefinition(
				"CATMA_BASE_TAG", "1", "CATMA Base Tag", "-16777216", null, true, fsdDecl );
		
		return fsdDecl;
	}
	
	private TeiElement addTagsetDefinition(String id, String nValue, TeiElement encodingDesc) {
		TeiElement fsdDecl = new TeiElement(TeiElementName.fsdDecl);
		fsdDecl.setID(id);
		fsdDecl.setAttributeValue(Attribute.n, nValue);
		encodingDesc.appendChild(fsdDecl);
		return fsdDecl;
	}
	
	private String getTimestamp() {
		Date date = new Date();
		return sdf.format(date);
	}

}

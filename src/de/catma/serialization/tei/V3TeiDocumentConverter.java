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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Nodes;
import de.catma.document.Range;
import de.catma.tag.Version;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;


public class V3TeiDocumentConverter implements TeiDocumentConverter {
	
	private static final class TagDef {
		private String color;
		private HashMap<String, String> properties;
		public TagDef(String color) {
			this.properties = new HashMap<String, String>();
			this.color = color;
		}
	}
	
	private static final Calendar LEGACY_VERSION_BASE_DATE = Calendar.getInstance();
	static {
		LEGACY_VERSION_BASE_DATE.set(2008, 1, 1, 0, 0, 0);
	}
	
	private static class OldInstance {
		private Range range;
		private String oldTagID;
		
		public OldInstance(Range range, String oldTagID) {
			this.range = range;
			this.oldTagID = oldTagID;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((oldTagID == null) ? 0 : oldTagID.hashCode());
			result = prime * result + ((range == null) ? 0 : range.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OldInstance other = (OldInstance) obj;
			if (oldTagID == null) {
				if (other.oldTagID != null)
					return false;
			} else if (!oldTagID.equals(other.oldTagID))
				return false;
			if (range == null) {
				if (other.range != null)
					return false;
			} else if (!range.equals(other.range))
				return false;
			return true;
		}
		
		
		
	}
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	private TeiElement standardTagsetDefinition;
	private HashMap<String,TeiElement> tagsetDefinitions = 
			new HashMap<String, TeiElement>();
	private HashMap<String,TagDef> tagDefinitions = 
			new HashMap<String,TagDef>();
	private IDGenerator catmaIDGenerator = new IDGenerator();
	
	private HashMap<String,Set<Range>> oldTagID2Ranges = 
			new HashMap<String, Set<Range>>();
	private HashMap<OldInstance,String> oldInstance2newInstanceID = 
			new HashMap<V3TeiDocumentConverter.OldInstance, String>();
	
	public void convert(TeiDocument teiDocument) {
	
		TeiElement encodingDesc = new TeiElement(TeiElementName.encodingDesc);
		((TeiElement)teiDocument.getNodes(TeiElementName.teiHeader).get(0)).appendChild(encodingDesc);
		

		Nodes tagElements = 
				teiDocument.getNodes(TeiElementName.fs, AttributeValue.type_catma_tag);
		
		if (tagElements.size() != 0) {
			String standardTagsetID = createStandardTagsetID(tagElements);
			standardTagsetDefinition = addStandardTagset(encodingDesc, standardTagsetID);
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
		
		Nodes segElements = teiDocument.getNodes(TeiElementName.seg);
		TeiElement text = (TeiElement)teiDocument.getNodes(TeiElementName.text).get(0);
		for (int i=0; i<segElements.size(); i++) {
			TeiElement segElement = (TeiElement)segElements.get(i);
			if ((segElement.getAttributeValue(Attribute.ana) != null) 
					&& !segElement.getAttributeValue(Attribute.ana).isEmpty()) {
				addTagInstance(segElement, text);
			}
		}
		
		adjustPointers(teiDocument);
		
		teiDocument.getTeiHeader().getTechnicalDescription().setVersion(TeiDocumentVersion.V3);
		
//		teiDocument.printXmlDocument();
	}

	private String createStandardTagsetID(Nodes tagElements) {
		// we try to generate an id from with the contents of the tagElements as a base
		// that should be pretty individual
		StringBuilder builder = new StringBuilder();
		for (int i=0; i<tagElements.size(); i++) {
			builder.append(tagElements.get(i).getValue());
		}
		
		return catmaIDGenerator.generate(builder.toString());
	}

	private void adjustPointers(TeiDocument teiDocument) {
		Nodes pointers = teiDocument.getNodes(TeiElementName.ptr);
		
		for( int i=0; i<pointers.size(); i++) {
			TeiElement pointer = (TeiElement)pointers.get(i);
			Pair<String,Range> target = getTarget(pointer);

			String newTarget = 
					"catma:///" + target.getFirst() + "#char=" 
							+ target.getSecond().getStartPoint()
							+ ","
							+ target.getSecond().getEndPoint();
			
			pointer.setAttributeValue(Attribute.ptr_target, newTarget);
		}
		
	}
	
	private Pair<String,Range> getTarget(TeiElement pointer) {
		String target = pointer.getAttributeValue(Attribute.ptr_target);
		
		String[] uri_points = target.split( "#" );
		String uri = uri_points[0].trim();
		String[] points = uri_points[1].split( "/." );
		try {
			uri = URLEncoder.encode(uri, "UTF8");
			Range r = 
					new Range(
						Integer.valueOf(points[1].substring( 
								0, points[1].indexOf( ',' ) ).trim()),
						Integer.valueOf(points[2].substring( 
								0, points[2].indexOf( ')' ) ).trim()));
			return new Pair<String, Range>(uri, r);
		}
		catch (UnsupportedEncodingException uee) {
			throw new IllegalStateException("UTF8 characterset not supported!");
		}
	}

	private void addTagInstance(TeiElement segElement, TeiElement text) {
		String references = segElement.getAttributeValue(Attribute.ana);
		String[] idValues = references.trim().split( "#" );
		StringBuilder newReferencesBuilder = new StringBuilder();
		
		for( String id : idValues ) {
			if (!id.trim().isEmpty()) {
				id = id.trim().toUpperCase();
				if (tagDefinitions.containsKey(id)) { // some broken files contain tagintances without a tagdef, we skip those
					Pair<String,Range> target = getTarget(
							segElement.getFirstTeiChildElement(TeiElementName.ptr));
				
					String instanceID = getInstanceID(id, target.getSecond());
					newReferencesBuilder.append(" #");
					newReferencesBuilder.append(instanceID);
	
					if (!oldInstance2newInstanceID.values().contains(instanceID)) {
						
						TagDef tagDefinition = tagDefinitions.get(id);
						
						TeiElement fs = new TeiElement(TeiElementName.fs);
						
						fs.setID(instanceID);
						fs.setAttributeValue(Attribute.type, id);
						
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
					this.oldInstance2newInstanceID.put(
							new OldInstance(target.getSecond(),id), 
							instanceID);
				}
			}
		}
		segElement.setAttributeValue(
				Attribute.ana, newReferencesBuilder.toString().trim());
	}

	private String getInstanceID(String oldTagID, Range currentRange) {
		
		if (oldTagID2Ranges.containsKey(oldTagID)) {
			Set<Range> ranges = oldTagID2Ranges.get(oldTagID);
			for (Range r : ranges) {
				if (currentRange.isAdjacentTo(r)) {
					ranges.add(currentRange);
					String instanceID = 
							this.oldInstance2newInstanceID.get(
									new OldInstance(r, oldTagID));
					return instanceID;
				}
			}
			ranges.add(currentRange);
		}
		else {
			HashSet<Range> ranges = new HashSet<Range>();
			ranges.add(currentRange);
			this.oldTagID2Ranges.put(oldTagID, ranges);
		}
		
		String newInstanceID = 
				catmaIDGenerator.generate(); 
		//just for testing to keep the id stable but unique:
//						currentRange.toString()+oldTagID);
		
		return newInstanceID;
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
						getTimestamp(
							Integer.valueOf(
								version.substring(0,version.indexOf('_')))),
						getTagName(properties),
						getColorValue(properties),
						baseTagID,
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
		fDecl.setID(catmaIDGenerator.generate(tagDefinition.getID()+propertyName));
		fDecl.setAttributeValue(Attribute.fDecl_name, propertyName);
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
			return "";
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
			String baseTagID,
			TeiElement tagsetDefinition) {
		TeiElement fsDecl = new TeiElement(TeiElementName.fsDecl);
		fsDecl.setID(id);
		fsDecl.setAttributeValue(Attribute.n, nValue);
		fsDecl.setAttributeValue(Attribute.type, id);
		if (!baseTagID.isEmpty()) {
			fsDecl.setAttributeValue(Attribute.fsDecl_baseTypes, baseTagID);
		}
		
		TeiElement fsDescr = new TeiElement(TeiElementName.fsDescr);
		fsDescr.appendChild(tagName);
		
		TeiElement fDecl = new TeiElement(TeiElementName.fDecl);
		fDecl.setID(catmaIDGenerator.generate(id+"catma_displaycolor"));
		fDecl.setAttributeValue(Attribute.fDecl_name, "catma_displaycolor");
		
		TeiElement vRange = new TeiElement(TeiElementName.vRange);
		
		TeiElement numeric = new TeiElement(TeiElementName.numeric);
		numeric.setAttributeValue(Attribute.numeric_value, colorValue);

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
		int splitPoint = nValue.lastIndexOf(' ');
		String tagsetName = nValue.substring(0, splitPoint); 
		String version =
				nValue.substring(splitPoint+1).substring(
						0, nValue.substring(splitPoint+1).indexOf('_'));
		return addTagsetDefinition(
			tagsetID, tagsetName+" "+getTimestamp(
					Integer.valueOf(version)), encodingDesc);
	}

	private TeiElement addStandardTagset(TeiElement encodingDesc, String standardTagsetID) {
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
		Version baseVersion = new Version(LEGACY_VERSION_BASE_DATE.getTime());
		
		TeiElement fsdDecl = 
				addTagsetDefinition(
						standardTagsetID, // there is no standard tagset in V3, so we create new "standard tagsets" with identical names
						"Standard Tagset "+baseVersion, encodingDesc );		
		return fsdDecl;
	}
	
	private TeiElement addTagsetDefinition(String id, String nValue, TeiElement encodingDesc) {
		TeiElement fsdDecl = new TeiElement(TeiElementName.fsdDecl);
		fsdDecl.setID(id);
		fsdDecl.setAttributeValue(Attribute.n, nValue);
		encodingDesc.appendChild(fsdDecl);
		return fsdDecl;
	}
	
	private String getTimestamp(int oldVersion) {
		if (oldVersion > 1) {
			Calendar tempCal = Calendar.getInstance();
			tempCal.setTimeInMillis(LEGACY_VERSION_BASE_DATE.getTimeInMillis());
			tempCal.add(Calendar.DAY_OF_YEAR, oldVersion);
			return sdf.format(tempCal.getTime());
		}
		
		return sdf.format(LEGACY_VERSION_BASE_DATE.getTime());
	}

}

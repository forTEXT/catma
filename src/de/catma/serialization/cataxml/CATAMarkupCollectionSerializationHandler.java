package de.catma.serialization.cataxml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import de.catma.document.Range;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.serialization.UserMarkupCollectionSerializationHandler;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyDefinition.SystemPropertyName;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Text;

public class CATAMarkupCollectionSerializationHandler implements UserMarkupCollectionSerializationHandler {
	private static final SimpleDateFormat DATEFORMAT = new SimpleDateFormat("dd/MM/yyyy"); 
	
	private static interface CATATagReference {

		public void appendTo(Element textElement);
		
	}
	
	private static class StartReference implements CATATagReference {
		private TagReference tagReference;

		public StartReference(TagReference tagReference) {
			super();
			this.tagReference = tagReference;
		}
		
		@Override
		public void appendTo(Element textElement) {
			Element codeStart = new Element("codeStart");
			
			//TODO: handle multiple tagrefs
			codeStart.addAttribute(new Attribute("key", tagReference.getTagInstanceID()));
			codeStart.addAttribute(new Attribute("name", tagReference.getTagDefinition().getName()));
			codeStart.addAttribute(new Attribute("type", tagReference.getTagDefinition().getUuid()));
			
			//TODO: CATMA doesn't provide annotation date yet, we use export date for now
			codeStart.addAttribute(new Attribute("date", DATEFORMAT.format(new Date())));
			codeStart.addAttribute(new Attribute(
				"coder",
				tagReference.getTagInstance().getSystemProperty(
					tagReference.getTagDefinition().getPropertyDefinitionByName(
						SystemPropertyName.catma_markupauthor.name()).getUuid()).getPropertyValueList().getFirstValue()));
			
			for (Property p : tagReference.getTagInstance().getUserDefinedProperties()) {
				codeStart.addAttribute(
					new Attribute(
						"userdefined_"+p.getPropertyDefinition().getUuid(), 
						p.getName() + ":" + p.getPropertyValueList().getFirstValue()));
			}
			
			textElement.appendChild(codeStart);
		}
		@Override
		public String toString() {
			return "START: " + tagReference;
		}
	}
	
	private static class EndReference implements CATATagReference {
		private TagReference tagReference;

		public EndReference(TagReference tagReference) {
			super();
			this.tagReference = tagReference;
		}
		
		@Override
		public void appendTo(Element textElement) {
			Element codeend = new Element("codeend");
			codeend.addAttribute(new Attribute("key", tagReference.getTagInstanceID()));
			textElement.appendChild(codeend);
		}
		
		@Override
		public String toString() {
			return "END: " + tagReference;
		}
	}
	
	
	private Document document;
	private String structureTagsetUuid;
	private TagsetDefinition structureTagsetDefintion;

	public CATAMarkupCollectionSerializationHandler() {
		this(new Document(new Element("project")), "");
	}
	
	public CATAMarkupCollectionSerializationHandler(String structureTagsetUuid) {
		this(new Document(new Element("project")), structureTagsetUuid);
	}

	public CATAMarkupCollectionSerializationHandler(Document document, String structureTagsetUuid) {
		super();
		this.document = document;
		this.structureTagsetUuid = structureTagsetUuid;
	}

	public CATAMarkupCollectionSerializationHandler(Document document) {
		this(document, "");
	}

	@Override
	public void serialize(UserMarkupCollection userMarkupCollection, SourceDocument sourceDocument,
			OutputStream outputStream) throws IOException {
		
		TagLibrary tagLibrary = userMarkupCollection.getTagLibrary();
		
		if (document.getRootElement().getFirstChildElement("codebook") == null) {
			CATATagLibrarySerializationHandler cataTagLibrarySerializationHandler = 
					new CATATagLibrarySerializationHandler(document, structureTagsetUuid);
			cataTagLibrarySerializationHandler.serialize(tagLibrary);
			structureTagsetUuid = 
					cataTagLibrarySerializationHandler.getStructureTagsetUuid();
			this.structureTagsetDefintion = tagLibrary.getTagsetDefinition(structureTagsetUuid);
		}
		else {
			Element structure = document.getRootElement().getFirstChildElement("structure");
			structureTagsetUuid = structure.getAttributeValue("id");
		}
		
		Element units = document.getRootElement().getFirstChildElement("units");
		
		if (units == null) {
			units = new Element("units");
			document.getRootElement().appendChild(units);
		}
		
		Element unit = new Element("unit");
		units.appendChild(unit);
		
		Element textElement = 
			addProperties(tagLibrary.getTagsetDefinition(
					structureTagsetUuid), userMarkupCollection, sourceDocument, unit);
		
		try {
			addAnnotatedText(sourceDocument, userMarkupCollection, tagLibrary, textElement);
		} catch (Exception e) {
			throw new IOException(e);
		}
		
	}

	private void addAnnotatedText(
			SourceDocument sourceDocument, 
			UserMarkupCollection userMarkupCollection,
			TagLibrary tagLibrary, Element textElement) throws Exception {
		
		TreeMap<Integer, List<CATATagReference>> offsetToCATATagReference = new TreeMap<>();
		mergeReferences(userMarkupCollection);

		TagsetDefinition structureTagsetDefinition = tagLibrary.getTagsetDefinition(structureTagsetUuid);
		for (TagReference tr : userMarkupCollection.getTagReferences()) {
			if (!structureTagsetDefinition.hasTagDefinition(tr.getTagDefinition().getUuid())) {
				if (!offsetToCATATagReference.containsKey(tr.getRange().getStartPoint())) {
					offsetToCATATagReference.put(tr.getRange().getStartPoint(), new ArrayList<>());
				}

				if (!offsetToCATATagReference.containsKey(tr.getRange().getEndPoint())) {
					offsetToCATATagReference.put(tr.getRange().getEndPoint(), new ArrayList<>());
				}
				
				offsetToCATATagReference.get(tr.getRange().getStartPoint()).add(new StartReference(tr));
				offsetToCATATagReference.get(tr.getRange().getEndPoint()).add(new EndReference(tr));
			}
 		}
		
		
		Integer lastPos = null;

		for (Map.Entry<Integer, List<CATATagReference>> entry : offsetToCATATagReference.entrySet()) {
			int pos = entry.getKey();

			if ((lastPos != null) && (pos > lastPos)) {
				Text chunk = new Text(sourceDocument.getContent(new Range(lastPos, pos)));
				textElement.appendChild(chunk);
			}
			else if (pos > 0) {
				Text chunk = new Text(sourceDocument.getContent(new Range(0, pos)));
				textElement.appendChild(chunk);
			}
			
			for (CATATagReference ref : entry.getValue()) {
				ref.appendTo(textElement);
			}
			
			lastPos = pos;
		}
		
		if (lastPos == null) {
			lastPos = 0;
		}
		
		if (lastPos < sourceDocument.getLength()) {
			Text chunk = new Text(sourceDocument.getContent(new Range(lastPos, sourceDocument.getLength())));
			textElement.appendChild(chunk);
		}
		
	}

	private void mergeReferences(UserMarkupCollection userMarkupCollection) throws Exception {
		Map<String, TagInstance> tagInstances = new HashMap<>();
		for (TagReference tr : userMarkupCollection.getTagReferences()) {
			tagInstances.put(tr.getTagInstanceID(), tr.getTagInstance());
		}
		
		for (TagInstance ti : tagInstances.values()) {
			List<TagReference> refs = userMarkupCollection.getTagReferences(ti);
			if (refs.size() > 1) {
				TreeSet<TagReference> sortedRefs = new TreeSet<>(new TagReference.RangeComparator());
				sortedRefs.addAll(refs);
				
				TagReference lastRef = null; 
				
				for (TagReference ref : sortedRefs) {
					if (lastRef != null) {
						if (lastRef.getRange().isAdjacentTo(ref.getRange())) {
							TagReference mergedRef = 
								new TagReference(
									lastRef.getTagInstance(), 
									lastRef.getTarget().toString(), 
									new Range(lastRef.getRange().getStartPoint(), 
											ref.getRange().getEndPoint()) );
							userMarkupCollection.removeTagReferences(Arrays.asList(lastRef, ref));
							userMarkupCollection.addTagReference(mergedRef);
							lastRef = mergedRef;
						}
						else {
							lastRef = ref;
						}
					}
					else {
						lastRef = ref;
					}
					
				}
			}
		}
	}

	private Element addProperties(
			TagsetDefinition tagsetDefinition, UserMarkupCollection userMarkupCollection, 
			SourceDocument sourceDocument, Element unit) {
		
		Element textElement = null;
		for (TagDefinition td : tagsetDefinition) {
			List<TagReference> tagReferences = userMarkupCollection.getTagReferences(td);
			if (!tagReferences.isEmpty()) {
				TagInstance ti = tagReferences.get(0).getTagInstance();
				PropertyDefinition typePropertyDefinition = 
						td.getPropertyDefinitionByName("type"); 
				
				Element property = new Element("property");
				property.addAttribute(new Attribute("name", td.getName()));
				property.addAttribute(new Attribute("id", ti.getUuid()));
				property.addAttribute(new Attribute(
					"author", 
					ti.getSystemProperty(
						td.getPropertyDefinitionByName(
							SystemPropertyName.catma_markupauthor.name()).getUuid()).getPropertyValueList().getFirstValue()));
				
				for (PropertyDefinition pd : td.getUserDefinedPropertyDefinitions()) {
					Property p = ti.getProperty(pd.getUuid());
					if ((p!=null) && (p.getPropertyValueList().getFirstValue() != null)) {
						property.addAttribute(new Attribute(p.getName(), p.getPropertyValueList().getFirstValue()));
					}
				}
				
				unit.appendChild(property);
				
				if (typePropertyDefinition.getFirstValue().equals(
						"text")) {
					textElement = new Element(td.getName());
				}
			}
		}
		
		if (textElement == null) {
			for (TagDefinition td : tagsetDefinition) {
				PropertyDefinition pd = td.getPropertyDefinitionByName("type");
				if (pd.getFirstValue().equals("text")) {
					Element property = new Element("property");
					property.addAttribute(new Attribute("name", td.getName()));

					property.addAttribute(new Attribute("id",td.getUuid()));
					property.addAttribute(new Attribute(
						"author", 
						td.getAuthor()));
					unit.appendChild(property);
					return property;
				}
				
			}

			textElement = new Element("property");
			textElement.addAttribute(new Attribute("name", "default-text"));
			textElement.addAttribute(new Attribute("TEXT", sourceDocument.toString()));
			unit.appendChild(textElement);
		}
		return textElement;
	}

	@Override
	public UserMarkupCollection deserialize(String id, InputStream inputStream) throws IOException {
		// TODO needs implementation
		throw new UnsupportedOperationException("not yet implemented");
	}

	public TagsetDefinition getStructureTagsetDefintion() {
		return structureTagsetDefintion;
	}
}

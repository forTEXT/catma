package de.catma.serialization.cataxml;

import java.io.IOException;
import java.io.InputStream;

import de.catma.serialization.TagLibrarySerializationHandler;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyDefinition.SystemPropertyName;
import de.catma.tag.PropertyPossibleValueList;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.util.IDGenerator;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;

public class CATATagLibrarySerializationHandler implements TagLibrarySerializationHandler {
	
	private Document document;
	private String structureTagsetUuid;

	public CATATagLibrarySerializationHandler() {
		this(new Document(new Element("project")), "");
	}
	
	public CATATagLibrarySerializationHandler(String structureTagsetUuid) {
		this(new Document(new Element("project")), structureTagsetUuid);
	}

	public CATATagLibrarySerializationHandler(Document document) {
		this(document, "");
	}
	
	public CATATagLibrarySerializationHandler(Document document, String structureTagsetUuid) {
		super();
		this.document = document;
		this.structureTagsetUuid = structureTagsetUuid;
	}

	@Override
	public void serialize(TagLibrary tagLibrary) throws IOException {
		Element projectNode = document.getRootElement();
		
		for (TagsetDefinition tsDef : tagLibrary.getTagsetDefinitions()) {
			if (!tsDef.getUuid().equals(structureTagsetUuid)) {
				Element codeBook = new Element("codebook");
				codeBook.addAttribute(new Attribute("name", tsDef.getName()));
				codeBook.addAttribute(new Attribute("id", tsDef.getUuid()));
				codeBook.addAttribute(new Attribute("version", tsDef.getVersion().toString()));
				
				projectNode.appendChild(codeBook);
				
				for (TagDefinition td : tsDef) {
					if (td.getParentUuid().isEmpty()) {
						addCategoryOrCode(td, tsDef, codeBook);
					}
				}
			}
		}
		
		TagsetDefinition structureDef = tagLibrary.getTagsetDefinition(structureTagsetUuid);
		if (structureDef == null) {
			IDGenerator idGenerator = new IDGenerator();
			structureDef = new TagsetDefinition(null, idGenerator.generate(), "default-structure", new Version());
			TagDefinition text = new TagDefinition(null, idGenerator.generate(), "TEXT", new Version(), null, null);
			
			PropertyDefinition type = new PropertyDefinition(null, idGenerator.generate(), "type", new PropertyPossibleValueList("text"));
			text.addUserDefinedPropertyDefinition(type);
			
			PropertyDefinition desc = new PropertyDefinition(null, idGenerator.generate(), "description", new PropertyPossibleValueList("default text container"));
			text.addUserDefinedPropertyDefinition(desc);
			
			text.addSystemPropertyDefinition(
				new PropertyDefinition(
					null,  
					idGenerator.generate(), 
					SystemPropertyName.catma_markupauthor.name(), 
					new PropertyPossibleValueList("converter")));
			
			structureDef.addTagDefinition(text);
			tagLibrary.add(structureDef);
			structureTagsetUuid = structureDef.getUuid();
		}
		
		addStructure(structureDef, projectNode);
		
	}

	private void addStructure(TagsetDefinition structureDef, Element parent) {
		Element structure = new Element("structure");
		structure.addAttribute(new Attribute("id", structureDef.getUuid()));
		structure.addAttribute(new Attribute("version", structureDef.getVersion().toString()));
		parent.appendChild(structure);
		
		for (TagDefinition td : structureDef) {
			Element property = new Element("property");
			property.addAttribute(new Attribute("name", td.getName()));
			property.addAttribute(new Attribute("id", td.getUuid()));
			property.addAttribute(new Attribute("author", td.getAuthor()));
			property.addAttribute(new Attribute("version", td.getVersion().toString()));
			
			PropertyDefinition type = td.getPropertyDefinitionByName("type");
			if (type != null) {
				property.addAttribute(new Attribute("type", type.getFirstValue()));
			}
			PropertyDefinition desc = td.getPropertyDefinition("description");
			if (desc != null) {
				property.addAttribute(new Attribute("description", desc.getFirstValue()));
			}
			
			structure.appendChild(property);
		}
		
	}

	private void addCategoryOrCode(TagDefinition td, TagsetDefinition tsDef, Element parent) {
		if (tsDef.getDirectChildren(td).isEmpty()) {
			addCode(td, parent);
		}
		else {
			addCategory(td, tsDef, parent);
		}
	}

	private void addCategory(TagDefinition td, TagsetDefinition tsDef, Element parent) {
		Element cat = new Element("category");
		cat.addAttribute(new Attribute("name", td.getName()));
		cat.addAttribute(new Attribute("color", td.getColor()));
		cat.addAttribute(new Attribute("id", td.getUuid()));
		cat.addAttribute(new Attribute("author", td.getAuthor()));
		cat.addAttribute(new Attribute("version", td.getVersion().toString()));
		
		parent.appendChild(cat);
		
		for (PropertyDefinition pd : td.getUserDefinedPropertyDefinitions()) {
			Element property = new Element("customproperty");
			property.addAttribute(new Attribute("name", pd.getName()));
			property.addAttribute(new Attribute("id", pd.getUuid()));
			
			if (!pd.getPossibleValueList().getPropertyValueList().getValues().isEmpty()) {
				property.addAttribute(
					new Attribute(
						"values", 
						pd.getPossibleValueList().getPropertyValueList().getValues().toString()));
			}
		}
		
		for (TagDefinition childTd : tsDef.getDirectChildren(td)) {
			addCategoryOrCode(childTd, tsDef, cat);
		}
	}

	private void addCode(TagDefinition td, Element parent) {
		Element code = new Element("code");
		code.addAttribute(new Attribute("name", td.getName()));
		code.addAttribute(new Attribute("color", td.getColor()));
		code.addAttribute(new Attribute("id", td.getUuid()));
		code.addAttribute(new Attribute("author", td.getAuthor()));
		code.addAttribute(new Attribute("version", td.getVersion().toString()));
		
		parent.appendChild(code);
		
		for (PropertyDefinition pd : td.getUserDefinedPropertyDefinitions()) {
			Element property = new Element("customproperty");
			property.addAttribute(new Attribute("name", pd.getName()));
			property.addAttribute(new Attribute("id", pd.getUuid()));
			
			if (!pd.getPossibleValueList().getPropertyValueList().getValues().isEmpty()) {
				property.addAttribute(
					new Attribute(
						"values", 
						pd.getPossibleValueList().getPropertyValueList().getValues().toString()));
			}
		}
	}

	@Override
	public TagLibrary deserialize(String id, InputStream inputStream) throws IOException {
		// TODO need implementation
		throw new UnsupportedOperationException("not yet implemented");
	}

	public String getStructureTagsetUuid() {
		return structureTagsetUuid;
	}

	public Document getDocument() {
		return document;
	}
}

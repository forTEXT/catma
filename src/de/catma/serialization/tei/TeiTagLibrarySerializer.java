package de.catma.serialization.tei;

import de.catma.core.tag.PropertyDefinition;
import de.catma.core.tag.TagDefinition;
import de.catma.core.tag.ITagLibrary;
import de.catma.core.tag.TagsetDefinition;

public class TeiTagLibrarySerializer {

	private TeiDocument teiDocument;

	public TeiTagLibrarySerializer(TeiDocument teiDocument) {
		this.teiDocument = teiDocument;
	}

	public void serialize(ITagLibrary tagLibrary) {
		TeiElement encodingDesc = 
				teiDocument.getTeiHeader().getEncodingDescElement();
		for (TagsetDefinition tagset : tagLibrary) {
			write(tagset, encodingDesc);
		}
	}

	private void write(TagsetDefinition tagset, TeiElement encodingDesc) {
	
		TeiElement fsdDecl = new TeiElement(TeiElementName.fsdDecl);
		fsdDecl.setID(tagset.getID());
		fsdDecl.setAttributeValue(
			Attribute.n, tagset.getName() + " " + tagset.getVersion());
		encodingDesc.appendChild(fsdDecl);
		
		for (TagDefinition td : tagset) {
			write(td, fsdDecl);
		}
	}

	private void write(TagDefinition td, TeiElement fsdDecl) {
		
		TeiElement fsDecl = new TeiElement(TeiElementName.fsDecl);
		fsDecl.setID(td.getID());
		fsDecl.setAttributeValue(Attribute.n, td.getVersion().toString());
		fsDecl.setAttributeValue(Attribute.type, td.getID());
		if (!td.getParentID().isEmpty()) {
			fsDecl.setAttributeValue(Attribute.fsDecl_baseTypes, td.getParentID());
		}
		fsdDecl.appendChild(fsDecl);
		
		TeiElement fsDescr = new TeiElement(TeiElementName.fsDescr);
		fsDescr.appendChild(td.getName());
		fsDecl.appendChild(fsDescr);
		
		for (PropertyDefinition pd : td.getSystemPropertyDefinitions()) {
			write(pd, fsDecl);
		}
		
		for (PropertyDefinition pd : td.getUserDefinedPropertyDefinitions()) {
			write(pd, fsDecl);
		}
	}

	private void write(PropertyDefinition pd, TeiElement fsDecl) {
		TeiElement fDecl = new TeiElement(TeiElementName.fDecl);
		fsDecl.appendChild(fDecl);
		fDecl.setID(pd.getId());
		fDecl.setAttributeValue(Attribute.fDecl_name, pd.getName());
		TeiElement vRange = new TeiElement(TeiElementName.vRange);
		fDecl.appendChild(vRange);
		for (String value :
			pd.getPossibleValueList().getPropertyValueList().getValues()) {

			TeiElement string = new TeiElement(TeiElementName.string);
			string.appendChild(value);
			vRange.appendChild(string);
		}
	}

}

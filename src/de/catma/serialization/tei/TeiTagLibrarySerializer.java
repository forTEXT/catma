package de.catma.serialization.tei;

import de.catma.core.tag.PropertyDefinition;
import de.catma.core.tag.TagDefinition;
import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagsetDefinition;

public class TeiTagLibrarySerializer {

	private TeiDocument teiDocument;

	public TeiTagLibrarySerializer(TeiDocument teiDocument) {
		this.teiDocument = teiDocument;
	}

	public void serialize(TagLibrary tagLibrary) {
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
		if (!td.getBaseID().isEmpty()) {
			fsDecl.setAttributeValue(Attribute.fsDecl_baseTypes, td.getBaseID());
		}
		TeiElement fsDescr = new TeiElement(TeiElementName.fsDescr);
		fsDescr.appendChild(td.getType());
		fsdDecl.appendChild(fsDecl);
		
		for (PropertyDefinition pd : td.getSystemPropertyDefinitions()) {
			write(pd, fsDecl);
		}
		
		for (PropertyDefinition pd : td.getUserDefinedPropertyDefinitions()) {
			write(pd, fsDecl);
		}
	}

	private void write(PropertyDefinition pd, TeiElement fsDecl) {
		TeiElement fDecl = new TeiElement(TeiElementName.fDecl);
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

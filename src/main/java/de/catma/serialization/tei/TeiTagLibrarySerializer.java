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

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;

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
		fsdDecl.setID(tagset.getUuid());
		fsdDecl.setAttributeValue(
			Attribute.n, tagset.getName() + " " + tagset.getVersion());
		encodingDesc.appendChild(fsdDecl);
		
		for (TagDefinition td : tagset) {
			write(td, fsdDecl);
		}
	}

	private void write(TagDefinition td, TeiElement fsdDecl) {
		
		TeiElement fsDecl = new TeiElement(TeiElementName.fsDecl);
		fsDecl.setID(td.getUuid());
		fsDecl.setAttributeValue(Attribute.n, td.getVersion().toString());
		fsDecl.setAttributeValue(Attribute.type, td.getUuid());
		if (!td.getParentUuid().isEmpty()) {
			fsDecl.setAttributeValue(Attribute.fsDecl_baseTypes, td.getParentUuid());
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
		fDecl.setID(pd.getName()); //TODO: id is obsolete but needs a probably a version change
		fDecl.setAttributeValue(Attribute.fDecl_name, Validator.SINGLETON.convertToXMLName(pd.getName()));
		TeiElement vRange = new TeiElement(TeiElementName.vRange);
		fDecl.appendChild(vRange);

		TeiElement vColl = new TeiElement(TeiElementName.vColl);
		vRange.appendChild(vColl);
		
		for (String value : pd.getPossibleValueList()) {

			TeiElement string = new TeiElement(TeiElementName.string);
			string.appendChild(value);
			vColl.appendChild(string);
		}
	}

}

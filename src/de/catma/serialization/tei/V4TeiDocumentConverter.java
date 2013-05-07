package de.catma.serialization.tei;

import nu.xom.Elements;
import nu.xom.Nodes;

public class V4TeiDocumentConverter implements TeiDocumentConverter {

	public void convert(TeiDocument teiDocument) {
		
		
		Nodes vRangeNodes = teiDocument.getNodes(TeiElementName.vRange);
		
		for (int i=0; i<vRangeNodes.size(); i++) {
			TeiElement vRangeNode = (TeiElement)vRangeNodes.get(i);
			
			if (vRangeNode.parentIs(TeiElementName.fDecl)) {
				Elements children = vRangeNode.getChildElements();
				
				if ((children.size() != 1) 
						|| ((children.size()==1) 
								&& (!((TeiElement)children.get(0)).is(TeiElementName.vColl)))) {
					TeiElement vColl = new TeiElement(TeiElementName.vColl);
					for (int j=0; j<children.size(); j++) {
						vRangeNode.removeChild(children.get(j));
						vColl.appendChild(children.get(j));
					}
					
					vRangeNode.appendChild(vColl);
				}
			}
			
		}
		
		teiDocument.getTeiHeader().getTechnicalDescription().setVersion(TeiDocumentVersion.V4);
		teiDocument.printXmlDocument();
	}
}

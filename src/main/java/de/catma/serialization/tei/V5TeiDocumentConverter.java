package de.catma.serialization.tei;

import nu.xom.Elements;
import nu.xom.Nodes;

public class V5TeiDocumentConverter implements TeiDocumentConverter {

	@Override
	public void convert(TeiDocument teiDocument) {
		Nodes vRangeNodes = teiDocument.getNodes(TeiElementName.vRange);
		
		for (int i=0; i<vRangeNodes.size(); i++) {
			TeiElement vRangeNode = (TeiElement)vRangeNodes.get(i);
			
			if (vRangeNode.parentIs(TeiElementName.f)) {
				Elements children = vRangeNode.getChildElements();
				
				if ((children.size() > 1) 
							&& (((TeiElement)children.get(0)).is(TeiElementName.vColl))) {
					TeiElement vColl = (TeiElement)children.get(0);
					if (!vColl.hasChildElements()) {
						for (int j=1; j<children.size(); j++) {
							vRangeNode.removeChild(children.get(j));
							vColl.appendChild(children.get(j));
						}
					}
				}
			}
			
		}
		
		teiDocument.getTeiHeader().getTechnicalDescription().setVersion(TeiDocumentVersion.V5);
//		teiDocument.printXmlDocument();

	}

}

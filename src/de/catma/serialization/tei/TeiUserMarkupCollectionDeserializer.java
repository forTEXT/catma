package de.catma.serialization.tei;

import java.util.ArrayList;
import java.util.List;

import nu.xom.Nodes;
import de.catma.core.document.standoffmarkup.user.TagReference;

public class TeiUserMarkupCollectionDeserializer {

	private TeiDocument teiDocument;
	private List<TagReference> tagReferences;

	public TeiUserMarkupCollectionDeserializer(TeiDocument teiDocument) {
		this.teiDocument = teiDocument;
		this.tagReferences = new ArrayList<TagReference>();
		deserialize();
	}

	private void deserialize() {
		Nodes segmentNodes = teiDocument.getNodes(
				TeiElementName.seg, AttributeValue.seg_ana_catma_tag_ref.getStartsWith());
		
		
		for (int i=0; i<segmentNodes.size(); i++) {
			TeiElement curSegment = (TeiElement)segmentNodes.get(i);
			AnaValueHandler anaValueHandler = new AnaValueHandler();
			List<String> tagInstanceIDs = 
					anaValueHandler.makeTagInstanceIDListFrom(curSegment.getAttributeValue(Attribute.ana));
			
		}
		
	}

}

package de.catma.ui.tagger.pager;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.TreeMultimap;

import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.TextRange;

public class AnnotationLayerBuilder {

	private List<TextRange> rangeParts;
	private Multimap<TextRange, ClientTagInstance> relativeTagInstanceByTextRange;
	private Table<Integer, TextRange, ClientTagInstance> layerTable;

	public AnnotationLayerBuilder(
			Map<String, ClientTagInstance> relativeTagInstanceByID, 
			List<TextRange> rangeParts) {

		this.rangeParts = rangeParts;
		
		relativeTagInstanceByTextRange = TreeMultimap.create(new Comparator<TextRange>() {
			@Override
			public int compare(TextRange o1, TextRange o2) {
				return o1.compareTo(o2);
			}
		},
		new Comparator<ClientTagInstance>() {
			@Override
			public int compare(ClientTagInstance o1, ClientTagInstance o2) {
				return o2.getLongestRangeSize()-o1.getLongestRangeSize();
			}
		});
		
		
		for (ClientTagInstance relativeTagInstance : relativeTagInstanceByID.values()) {
			for (TextRange textRange : relativeTagInstance.getRanges()) {
				for (TextRange rangePart : rangeParts) {
					if (rangePart.isCoveredBy(textRange)) {
						relativeTagInstanceByTextRange.put(rangePart, relativeTagInstance);
					}
				}
			}
		}
		
		build();
	}

	private void build() {
		layerTable = HashBasedTable.create(); 
		for (TextRange textRange : rangeParts) {
			for (ClientTagInstance relativeTagInstance : relativeTagInstanceByTextRange.get(textRange)) {
				int rowIdx = 0;
				while (layerTable.contains(rowIdx, textRange)) {
					rowIdx++;
				}
				
				layerTable.put(rowIdx, textRange, relativeTagInstance);
			}
		}
	}
	
	public Table<Integer, TextRange, ClientTagInstance> getLayerTable() {
		return layerTable;
	}
	

}

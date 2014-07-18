package de.catma.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.shared.ui.MultiSelectMode;
import com.vaadin.ui.TreeTable;

public class MultiSelectTreeTable extends TreeTable {
	public MultiSelectTreeTable() {
	}

	
//	@Override
//	public void changeVariables(Object source, Map<String, Object> variables) {
//		final String[] ranges = (String[]) variables.get("selectedRanges");
//
//		super.changeVariables(source, variables);
//        
//        if (isSelectable() 
//        		&& isMultiSelect()
//                && variables.containsKey("selected")
//                && getMultiSelectMode() == MultiSelectMode.DEFAULT
//                && ranges.length > 0) {
//	        @SuppressWarnings("unchecked")
//	        HashSet<Object> newValue = new LinkedHashSet<Object>(
//	                (Collection<Object>) getValue());
//
//	        for (String range : ranges) {
//                String[] split = range.split("-");
//                Object startItemId = itemIdMapper.get(split[0]);
//
//                Object previousItemId = findPreviousItemId(startItemId);
//                if (previousItemId != null) {
//	                Collection<?> gapItemIds = getGapItemIds(previousItemId, startItemId);
//	                newValue.addAll(gapItemIds);
//                }
//	        }
//	        
//	        setValue(newValue, true);
//        }
//	}


	private Collection<?> getGapItemIds(Object startGapItemId,
			Object endGapItemId) {
		List<Object> gapItemIds = new ArrayList<Object>();
		boolean withinGap = false;
		for (Object itemId : getItemIds()) {
			if (itemId.equals(startGapItemId)) {
				withinGap = true;
			}
			else if (itemId.equals(endGapItemId)) {
				return gapItemIds;
			}
			if (withinGap) {
				gapItemIds.add(itemId);
			}
		}
		return gapItemIds;
	}


	private Object findPreviousItemId(Object itemId) {
		Object tempPreviousItemId= null;
		
		Object value = getValue();
		if (value != null) {
			for (Object curItemId : (Set<?>)value) {
				if (curItemId.equals(itemId)) {
					return tempPreviousItemId;
				}
				tempPreviousItemId = curItemId;
			}
		}		
		return null;
	}

}
package de.catma.serialization.tei;

import java.util.ArrayList;
import java.util.List;

import nu.xom.Elements;
import de.catma.core.ExceptionHandler;

public class ValueRangePropertyValueFactory implements PropertyValueFactory {
	
	private boolean singleSelectValue = true;

	public String getValue(TeiElement teiElement) {
	
		StringBuilder builder = new StringBuilder();
		
		List<String> list = getValueAsList(teiElement);
		for (int i=0; i<list.size(); i++) {
			if (i>1) {
				builder.append(",");
			}
			builder.append(list.get(i));
		}
		
		if (list.size() > 0) {
			return builder.toString();
		}
		else {
			return null;
		}
	}

	public void setValue(TeiElement teiElement, Object value) {
		singleSelectValue = true;
		// TODO: implement
	}

	public List<String> getValueAsList(TeiElement teiElement) {
		singleSelectValue = true;
		
		ArrayList<String> result = new ArrayList<String>();
		
		Elements elements = teiElement.getChildElements();
		TeiElement vRange = (TeiElement)elements.get(0);
		
		Elements children = vRange.getChildElements();
		for (int i=0; i<children.size(); i++) {
			try {
				TeiElement curChild = (TeiElement)children.get(i);
				if (curChild.is(TeiElementName.numeric)) {
					result.add(new NumericPropertyValueFactory().getValue(vRange));
				}
				else if (curChild.is(TeiElementName.string)) {
					result.add(new StringPropertyValueFactory().getValue(vRange));
				}
				else {
					throw new UnknownElementException(curChild.getLocalName() + " is not supported!");
				}
			} catch (UnknownElementException e) {
				ExceptionHandler.log(e);
			}
		}
		
		singleSelectValue = result.size() > 1;
		
		return result;
	}

	public boolean isSingleSelectValue() {
		return singleSelectValue;
	}

}

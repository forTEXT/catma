package de.catma.ui.analyzenew.treehelper;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.vaadin.ui.UI;

import de.catma.document.Range;
import de.catma.indexer.KeywordInSpanContext;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.tag.TagDefinition;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.ui.util.Cleaner;
import de.catma.util.ColorConverter;

public class SingleItem implements TreeRowItem {

	private String forward;
	private String backward;
	private int position;
	private String treeKey;
	private String phrase;
	private String propertyName;
	private String propertyValue;
	private String query;
	private QueryResultRowArray queryResultRowArray;
	private QueryResultRow queryResultRow;
	private Range range;
	private int rangesHash;
	private Set<Range> ranges;
	
	private static final int SMALL_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH = 30;
	private static final int LARGE_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH = 300;
	static final String HORIZONTAL_ELLIPSIS = "\u2026";
	static final int MAX_VALUE_LENGTH = 10;
	static final int maxLength = 50;

	public SingleItem() {
		ranges = new TreeSet<Range>();
	}

	public void setTreeKey(String treeKey) {
		this.treeKey = treeKey;
	}

	public void setPhrase(String keyword) {
		phrase = keyword;
	}

	@Override
	public String getTreeKey() {
		return treeKey;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	@Override
	public int getFrequency() {
		return 1;
	}

	@Override
	public QueryResultRowArray getRows() {
		return queryResultRowArray;
	}

	public void setRows(QueryResultRowArray queryResultRowArray) {
		this.queryResultRowArray = queryResultRowArray;
		queryResultRow = queryResultRowArray.get(0);
		if (queryResultRow.getClass() == TagQueryResultRow.class) {
			TagQueryResultRow tQRR = (TagQueryResultRow) queryResultRow;
			range = tQRR.getRange();
			ranges = tQRR.getRanges();
			rangesHash = ranges.hashCode();
		}
	}

	@Override
	public String getArrowIcon() {
		return null;
	}

	public String getShortenTreeKey() {
		return shorten(this.treeKey, 18);
	}

	private String shorten(String toShortenValue, int maxLength) {
		if (toShortenValue.length() <= maxLength) {
			return toShortenValue;
		}
		return toShortenValue.substring(0, maxLength / 2) + "[" + HORIZONTAL_ELLIPSIS + "]"
				+ toShortenValue.substring(toShortenValue.length() - ((maxLength / 2) - 2), toShortenValue.length());
	}

	public String getPhrase() {
		return phrase;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public String getPropertyValue() {
		return propertyValue;

	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}

	public String getBackward() {
		return backward;
	}

	public void setBackward(String backward) {
		this.backward = backward;
	}

	public String getForward() {
		return forward;
	}

	public void setForward(String forward) {
		this.forward = forward;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public QueryResultRowArray getQueryResultRowArray() {
		return queryResultRowArray;
	}

	public void setQueryResultRowArray(QueryResultRowArray queryResultRowArray) {
		this.queryResultRowArray = queryResultRowArray;
		queryResultRow = queryResultRowArray.get(0);
		if (queryResultRow.getClass() == TagQueryResultRow.class) {
			TagQueryResultRow tQRR = (TagQueryResultRow) queryResultRow;
			phrase = tQRR.getPhrase();
		}

	}

	@Override
	public String getContext() {
		return getBackward() + getPhrase() + getForward();
	}
	
	private String buildKwicString() {
		String conc ="";
		
		StringBuilder builder = new StringBuilder();
	
				builder.append(
						Cleaner.clean(getBackward()));
				builder.append("<span");
				builder.append(" class=\"annotation-details-tag-color\"");
				builder.append(" style=\"");
				builder.append(" background-color:");
				builder.append("#cacfd2");
				builder.append(";");
				builder.append(" color: cacfd2");
				builder.append(";");
				builder.append("\">");
				
				builder.append(
						Cleaner.clean(
				
									getPhrase()
									));

				
				
				builder.append("</span>");	
				builder.append(
						Cleaner.clean(getForward()));
				return builder.toString();
			
			
			}
			
		
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((backward == null) ? 0 : backward.hashCode());
		result = prime * result + ((forward == null) ? 0 : forward.hashCode());
		result = prime * result + ((phrase == null) ? 0 : phrase.hashCode());
		result = prime * result + position;
		result = prime * result + ((propertyName == null) ? 0 : propertyName.hashCode());
		result = prime * result + ((propertyValue == null) ? 0 : propertyValue.hashCode());
		result = prime * result + ((query == null) ? 0 : query.hashCode());
		result = prime * result + ((queryResultRow == null) ? 0 : queryResultRow.hashCode());
		result = prime * result + ((queryResultRowArray == null) ? 0 : queryResultRowArray.hashCode());
		result = prime * result + ((range == null) ? 0 : range.hashCode());
		result = prime * result + ((ranges == null) ? 0 : ranges.hashCode());
		result = prime * result + rangesHash;
		result = prime * result + ((treeKey == null) ? 0 : treeKey.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SingleItem other = (SingleItem) obj;
		if (backward == null) {
			if (other.backward != null)
				return false;
		} else if (!backward.equals(other.backward))
			return false;
		if (forward == null) {
			if (other.forward != null)
				return false;
		} else if (!forward.equals(other.forward))
			return false;
		if (phrase == null) {
			if (other.phrase != null)
				return false;
		} else if (!phrase.equals(other.phrase))
			return false;
		if (position != other.position)
			return false;
		if (propertyName == null) {
			if (other.propertyName != null)
				return false;
		} else if (!propertyName.equals(other.propertyName))
			return false;
		if (propertyValue == null) {
			if (other.propertyValue != null)
				return false;
		} else if (!propertyValue.equals(other.propertyValue))
			return false;
		if (query == null) {
			if (other.query != null)
				return false;
		} else if (!query.equals(other.query))
			return false;
		if (queryResultRow == null) {
			if (other.queryResultRow != null)
				return false;
		} else if (!queryResultRow.equals(other.queryResultRow))
			return false;
		if (queryResultRowArray == null) {
			if (other.queryResultRowArray != null)
				return false;
		} else if (!queryResultRowArray.equals(other.queryResultRowArray))
			return false;
		if (range == null) {
			if (other.range != null)
				return false;
		} else if (!range.equals(other.range))
			return false;
		if (ranges == null) {
			if (other.ranges != null)
				return false;
		} else if (!ranges.equals(other.ranges))
			return false;
		if (rangesHash != other.rangesHash)
			return false;
		if (treeKey == null) {
			if (other.treeKey != null)
				return false;
		} else if (!treeKey.equals(other.treeKey))
			return false;
		return true;
	}

	@Override
	public String getContextDiv() {
		
		return buildKwicString();
	}

}

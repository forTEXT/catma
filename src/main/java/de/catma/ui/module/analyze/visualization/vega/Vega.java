package de.catma.ui.module.analyze.visualization.vega;

import java.util.List;

import com.google.common.collect.Lists;
import com.vaadin.data.HasValue;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.AbstractComponent;

import de.catma.document.Range;
import de.catma.queryengine.QueryId;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.client.ui.visualization.vega.VegaClientRpc;
import de.catma.ui.client.ui.visualization.vega.VegaServerRpc;
import de.catma.ui.client.ui.visualization.vega.VegaState;
import de.catma.ui.client.ui.visualization.vega.shared.SelectedQueryResultRow;

public class Vega extends AbstractComponent {
	
	public static class QueryResultValue implements HasValue<QueryResult> {
		private QueryResult queryResult;
		
		public QueryResultValue(QueryResult queryResult) {
			super();
			this.queryResult = queryResult;
		}

		@Override
		public void setValue(QueryResult value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public QueryResult getValue() {
			return this.queryResult;
		}

		@Override
		public Registration addValueChangeListener(ValueChangeListener<QueryResult> listener) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setRequiredIndicatorVisible(boolean requiredIndicatorVisible) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isRequiredIndicatorVisible() {
			return false;
		}

		@Override
		public void setReadOnly(boolean readOnly) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isReadOnly() {
			return true;
		}
		
	}
	
	private VegaServerRpc vegaServerRpc = new VegaServerRpc() {
		
		@Override
		public void onUserSelection(List<SelectedQueryResultRow> selection) {
			if (valueChangeListener != null) {
				QueryResultRowArray result = new QueryResultRowArray();
				for (SelectedQueryResultRow row : selection) {
					if (row.getAnnotationId() != null) {
						result.add(
							new TagQueryResultRow(
									QueryId.fromString(row.getQueryId()),
									row.getSourceDocumentId(), 
									Lists.newArrayList(new Range(row.getStartOffset(), row.getEndOffset())),
									row.getAnnotationCollectionId(),
									row.getTagId(), 
									row.getTagPath(),
									row.getTagVersion(), 
									row.getAnnotationId(), 
									row.getPropertyId(), 
									row.getPropertyName(),
									row.getPropertyValue()));
					}
					else {
						result.add(
							new QueryResultRow(
									QueryId.fromString(row.getQueryId()),
									row.getSourceDocumentId(), 
									new Range(row.getStartOffset(), row.getEndOffset()), 
									row.getPhrase()));
					}
				}
				
				valueChangeListener.valueChange(
					new ValueChangeEvent<QueryResult>(Vega.this, new QueryResultValue(result), null, true));
				
			}
		}
	};
	
	private ValueChangeListener<QueryResult> valueChangeListener;
	
	public Vega() {
		registerRpc(vegaServerRpc);
	}
	
	public void setVegaSpec(String vegaSpec) {
		this.getState().vegaSpec = vegaSpec;
	}

	
	@Override
	protected VegaState getState() {
		return (VegaState)super.getState();
	}
	
	public void setValueChangeListener(ValueChangeListener<QueryResult> valueChangeListener) {
		this.valueChangeListener = valueChangeListener;
	}
	
	public void reloadData() {
		getRpcProxy(VegaClientRpc.class).reloadData();
	}
}

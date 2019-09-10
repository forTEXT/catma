package de.catma.ui.module.analyze.queryresultpanel;

import de.catma.queryengine.result.QueryResultRowArray;

public enum DisplaySetting {
	GROUPED_BY_PHRASE(new QueryResultPanelHandler() {

		@Override
		public void init(QueryResultPanel queryResultPanel) {
			queryResultPanel.initPhraseBasedData();
		}

		@Override
		public void addQueryResultRootItems(QueryResultPanel queryResultPanel, QueryResultRowArray rows) {
			queryResultPanel.addPhraseBasedRootItems(rows);
		}
		
	}),
	GROUPED_BY_TAG(new QueryResultPanelHandler() {

		@Override
		public void init(QueryResultPanel queryResultPanel) {
			queryResultPanel.initTagBasedData();
		}

		@Override
		public void addQueryResultRootItems(QueryResultPanel queryResultPanel, QueryResultRowArray rows) {
			queryResultPanel.addTagBasedRootItems(rows);
		}
		
	}),
	ANNOTATIONS_AS_FLAT_TABLE(new QueryResultPanelHandler() {

		@Override
		public void init(QueryResultPanel queryResultPanel) {
			queryResultPanel.initFlatTagBasedData();
		}

		@Override
		public void addQueryResultRootItems(QueryResultPanel queryResultPanel, QueryResultRowArray rows) {
			queryResultPanel.addFlatTagBasedRootItems(rows);
		}
		
	}),
	PROPERTIES_AS_COLUMNS(new QueryResultPanelHandler() {

		@Override
		public void init(QueryResultPanel queryResultPanel) {
			queryResultPanel.initPropertiesAsColumnsTagBasedData();
		}

		@Override
		public void addQueryResultRootItems(QueryResultPanel queryResultPanel, QueryResultRowArray rows) {
			queryResultPanel.addPropertiesAsColumnsTagBasedRootItems(rows);
		}
		
	}),
	;
	private static interface QueryResultPanelHandler {
		public void init(QueryResultPanel queryResultPanel);
		public void addQueryResultRootItems(QueryResultPanel queryResultPanel, QueryResultRowArray rows);
	}
	
	private QueryResultPanelHandler initializationHandler;

	private DisplaySetting(QueryResultPanelHandler initializationHandler) {
		this.initializationHandler = initializationHandler;
	}
	
	public void init(QueryResultPanel queryResultPanel) {
		initializationHandler.init(queryResultPanel);
	}
	
	public void addQueryResultRootItems(QueryResultPanel queryResultPanel, QueryResultRowArray rows) {
		initializationHandler.addQueryResultRootItems(queryResultPanel, rows);
	}
	
}

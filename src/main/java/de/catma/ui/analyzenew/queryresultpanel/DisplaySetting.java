package de.catma.ui.analyzenew.queryresultpanel;

public enum DisplaySetting {
	GROUPED_BY_PHRASE(queryResultPanel -> queryResultPanel.initPhraseBasedData()),
	GROUPED_BY_TAG(queryResultPanel -> queryResultPanel.initTagBasedData()),
	ANNOTATIONS_AS_FLAT_TABLE(queryResultPanel -> queryResultPanel.initFlatTagBasedData()),
	PROPERTIES_AS_COLUMNS(queryResultPanel -> queryResultPanel.initPropertiesAsColumnsTagBasedData()),
	;
	private static interface InitializationHandler {
		public void initQueryResultPanel(QueryResultPanel queryResultPanel);
	}
	
	private InitializationHandler initializationHandler;

	private DisplaySetting(InitializationHandler initializationHandler) {
		this.initializationHandler = initializationHandler;
	}
	
	public void initQueryResultPanel(QueryResultPanel queryResultPanel) {
		initializationHandler.initQueryResultPanel(queryResultPanel);
	}
	
}

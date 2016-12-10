package de.catma.ui;

public enum ParameterComponentValue {
	TAGGER( catmaApplication -> {
		String sourceDocumentId = catmaApplication.getParameter(Parameter.TAGGER_DOCUMENT);
		
		catmaApplication.openSourceDocument(sourceDocumentId);
	})
	;
	
	private static interface Handler {
		public void show(CatmaApplication catmaApplication);
	}
	
	private Handler handler;
	
	private ParameterComponentValue(Handler handler) {
		this.handler = handler;
	}
	
	public void show(CatmaApplication catmaApplication) {
		handler.show(catmaApplication);
	}
	
	public static void show(CatmaApplication catmaApplication, String componentValue) {
		if ((componentValue == null) || componentValue.trim().isEmpty()) {
			return;
		}
		for (ParameterComponentValue pcv : values()) {
			if (pcv.name().toLowerCase().equals(componentValue.trim().toLowerCase())) {
				pcv.show(catmaApplication);
			}
		}
	}
}

package de.catma.ui.module.project.documentwizard;

public enum TagsetImportState {
	WILL_BE_CREATED("will be created"),
	WILL_BE_MERGED("will be merged"),
	WILL_BE_IGNORED("will be ignored"),
	;
	
	private String label;

	private TagsetImportState(String label) {
		this.label = label;
	}
	
	@Override
	public String toString() {
		return label;
	}
	
}
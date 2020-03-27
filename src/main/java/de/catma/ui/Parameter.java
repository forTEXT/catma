package de.catma.ui;

public enum Parameter {
	USER_IDENTIFIER("catma.user.identifier"), //$NON-NLS-1$
	USER_SPAWN("catma.user.spawn"), //$NON-NLS-1$
	USER_SPAWN_ASGUEST("catma.user.spawn.asguest"), //$NON-NLS-1$
	CORPORA_COPY("catma.corpora.copy"), //$NON-NLS-1$
	TAGLIBS_COPY("catma.taglibs.copy"), //$NON-NLS-1$
	COMPONENT("catma.component"), //$NON-NLS-1$
	TAGGER_DOCUMENT("catma.tagger.document"), //$NON-NLS-1$
	TAGGER_TAGSETDEF("catma.tagger.tsduuid"), //$NON-NLS-1$
	AUTOLOGIN("catma.autologin"),  //$NON-NLS-1$
	EXPERT("catma.expert"),
	;
	
	private String key;

	private Parameter(String key) {
		this.key = key;
	}
	
	public String getKey() {
		return key;
	}
}

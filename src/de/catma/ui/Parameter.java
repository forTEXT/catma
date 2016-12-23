package de.catma.ui;

public enum Parameter {
	USER_IDENTIFIER("catma.user.identifier"),
	USER_SPAWN("catma.user.spawn"),
	USER_SPAWN_ASGUEST("catma.user.spawn.asguest"),
	CORPORA_COPY("catma.corpora.copy"),
	TAGLIBS_COPY("catma.taglibs.copy"),
	COMPONENT("catma.component"),
	TAGGER_DOCUMENT("catma.tagger.document"),
	AUTOLOGIN("catma.autologin"),
	;
	
	private String key;

	private Parameter(String key) {
		this.key = key;
	}
	
	public String getKey() {
		return key;
	}
}

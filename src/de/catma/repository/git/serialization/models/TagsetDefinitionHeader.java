package de.catma.repository.git.serialization.models;

import de.catma.tag.Version;

public class TagsetDefinitionHeader extends HeaderBase {
	private Version version;

	public TagsetDefinitionHeader(String name, String description, Version version) {
		super(name, description);
		this.version = version;
	}

	public String getVersion() {
		return this.version.toString();
	}

	public void setSourceDocumentId(String versionString) {
		this.version = new Version(versionString);
	}
}

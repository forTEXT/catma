package de.catma.repository.git.serialization.models;

import com.jsoniter.annotation.JsonIgnore;
import de.catma.tag.Version;

public class TagsetDefinitionHeader extends HeaderBase {
	private Version version;

	public TagsetDefinitionHeader() {
		super();
	}

	public TagsetDefinitionHeader(String name, String description, Version version) {
		super(name, description);
		this.version = version;
	}

	public String getVersion() {
		return this.version.toString();
	}

	public void setVersion(String versionString) {
		this.version = new Version(versionString);
	}

	@JsonIgnore
	public Version version() {
		return this.version;
	}
}

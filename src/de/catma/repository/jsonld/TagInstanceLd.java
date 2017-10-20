package de.catma.repository.jsonld;

import com.jsoniter.annotation.JsonIgnore;
import com.jsoniter.annotation.JsonProperty;

public class TagInstanceLd {

	@JsonProperty(from = "@context", to = "@context")
	public String context;

	public String type;

	public String id;

	public TagInstanceLdBody body;

	public TagInstanceLdTarget target;

	@JsonIgnore
	public String getTagInstanceUuid() {
		int startIndex = this.id.indexOf("CATMA_");
		return this.id.substring(startIndex);
	}
}

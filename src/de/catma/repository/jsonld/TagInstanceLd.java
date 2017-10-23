package de.catma.repository.jsonld;

import com.jsoniter.annotation.JsonIgnore;
import com.jsoniter.annotation.JsonProperty;

public class TagInstanceLd {
	private String context;
	private String type;
	private String id;
	private TagInstanceLdBody body;
	private TagInstanceLdTarget target;

	@JsonProperty(to="@context")
	public String getContext() {
		return this.context;
	}

	@JsonProperty(from="@context")
	public void setContext(String context) {
		this.context = context;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public TagInstanceLdBody getBody() {
		return this.body;
	}

	public void setBody(TagInstanceLdBody body) {
		this.body = body;
	}

	public TagInstanceLdTarget getTarget() {
		return this.target;
	}

	public void setTarget(TagInstanceLdTarget target) {
		this.target = target;
	}

	@JsonIgnore
	public String getTagInstanceUUID() {
		int startIndex = this.id.indexOf("CATMA_");
		return this.id.substring(startIndex);
	}
}

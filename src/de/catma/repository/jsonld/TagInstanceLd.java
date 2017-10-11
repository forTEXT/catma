package de.catma.repository.jsonld;

import com.jsoniter.annotation.JsonCreator;
import com.jsoniter.annotation.JsonProperty;

public class TagInstanceLd {

	@JsonProperty(from = "@context", to = "@context")
	public String context;

	public String type;

	public String id;

	public TagInstanceLdBody body;

	public TagInstanceLdTarget target;

	public String getTagInstanceUuid(){
		int startIndex = this.id.indexOf("CATMA_");

		return this.id.substring(startIndex);
	};
}

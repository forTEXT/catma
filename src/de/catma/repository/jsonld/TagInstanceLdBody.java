package de.catma.repository.jsonld;

import com.jsoniter.annotation.JsonIgnore;
import com.jsoniter.annotation.JsonProperty;

import java.util.TreeMap;

public class TagInstanceLdBody {
	private TreeMap<String, String> context;
	private String type;
	private String tag;
	private TreeMap<String, String> properties;

	public TagInstanceLdBody() {
		this.context = new TreeMap<>();
		this.properties = new TreeMap<>();
	}

	@JsonProperty(to="@context")
	public TreeMap<String, String> getContext() {
		return this.context;
	}

	@JsonProperty(from="@context")
	public void setContext(TreeMap<String, String> context) {
		this.context = context;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTag() {
		return this.tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public TreeMap<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(TreeMap<String, String> properties) {
		this.properties = properties;
	}

	@JsonIgnore
	public String getTagDefinitionUUID() {
		int startIndex = this.tag.indexOf("CATMA_");
		return this.tag.substring(startIndex);
	}

	@JsonIgnore
	public String getPropertyDefinitionUUID(String propertyName) {
		String contextValue = this.context.get(propertyName);
		String array[] = contextValue.split("/");
		return array[array.length-1];
	}
}

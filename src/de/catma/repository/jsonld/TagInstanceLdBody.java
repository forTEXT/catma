package de.catma.repository.jsonld;

import com.jsoniter.annotation.JsonIgnore;
import com.jsoniter.annotation.JsonProperty;

import java.util.HashMap;

public class TagInstanceLdBody {
	public TagInstanceLdBody(){
		this.context = new HashMap<>();
		this.properties = new HashMap<>();
	}

	@JsonProperty(from = "@context", to = "@context")
	public HashMap<String, String> context;

	public String type;

	public String tag;

	public HashMap<String, String> properties;

	@JsonIgnore
	public String getTagDefinitionUuid(){
		int startIndex = this.tag.indexOf("CATMA_");

		return this.tag.substring(startIndex);
	};

	@JsonIgnore
	public String getPropertyDefinitionUuid(String propertyName){
		String contextValue = this.context.get(propertyName);
		String array[] = contextValue.split("/");

		return array[array.length-1];
	}
}

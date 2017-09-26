package de.catma.repository.jsonld;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.utils.JsonUtils;
import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.TagReference;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class TagReferenceJsonLd {
	private final Logger logger;
	private TagReference tagReference;

	public TagReferenceJsonLd(TagReference tagReference){
		this.logger = Logger.getLogger(this.getClass().getName());

		this.tagReference = tagReference;
	}

	public TagReference getTagReference(){
		return this.tagReference;
	}

	public String SerializeToJsonLd(){
		return "";
	}

	public static TagReferenceJsonLd DeserializeFromJsonLd(InputStream inputStream) throws IOException, URISyntaxException {
		Object jsonObject = JsonUtils.fromInputStream(inputStream);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.valueToTree(jsonObject);

		Logger.getLogger("TagReferenceJsonLd").info(JsonUtils.toPrettyString(jsonObject));

		String uri = node.get("target").get("source").asText();

		int startPoint = node.get("target").get("TextPositionSelector").get("start").asInt();
		int endPoint =  node.get("target").get("TextPositionSelector").get("end").asInt();
		Range range = new Range(startPoint, endPoint);

		// TagInstance tagInstance, String uri, Range range
		TagReference internalReference = new TagReference(null, uri, range);
		return new TagReferenceJsonLd(internalReference);
	}
}

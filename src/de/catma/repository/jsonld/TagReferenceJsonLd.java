package de.catma.repository.jsonld;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.utils.JsonUtils;
import com.jsoniter.JsonIterator;
import com.jsoniter.annotation.JsonCreator;
import com.jsoniter.annotation.JsonProperty;
import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;
import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.tag.TagInstance;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
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

	public static TagReferenceJsonLd Deserialize (InputStream inputStream) throws IOException, URISyntaxException {
		JsonIterator iter = JsonIterator.parse(inputStream, 128);

		TagInstanceLd tagInstanceLd = iter.read(TagInstanceLd.class);
		iter.close();

		// Need to fetch the Tag definition
//		TagInstance tagInstance = new TagInstance(tagInstanceLd.getUuidFromId(), null);

		Range range = new Range(tagInstanceLd.target.TextPositionSelector.start, tagInstanceLd.target.TextPositionSelector.end);
		TagReference internalReference = new TagReference(null, tagInstanceLd.target.source, range);

		return new TagReferenceJsonLd(internalReference);
	}

	public String Serialize(){
		TagInstanceLd tagInstanceLd = new TagInstanceLd();
		tagInstanceLd.context = "http://www.w3.org/ns/anno.jsonld";
		tagInstanceLd.type = "Annotation";
		tagInstanceLd.id = "http://catma.de/portal/annotation/" + this.tagReference.getTagInstance().getUuid();

		tagInstanceLd.body = new TagInstanceLdBody();
		// TODO: loop over properties and add them to the context

		tagInstanceLd.body.context.put("tag", "http://catma.de/portal/tag");
		tagInstanceLd.body.type = "Dataset";
		tagInstanceLd.body.tag = "http://catma.de/portal/tag/" + this.tagReference.getTagDefinition().getUuid();

		// TODO: Loop over properties and add them to the body property collection

		tagInstanceLd.target = new TagInstanceLdTarget();
		tagInstanceLd.target.source = this.tagReference.getTarget().toString();
		tagInstanceLd.target.TextPositionSelector.start = this.tagReference.getRange().getStartPoint();
		tagInstanceLd.target.TextPositionSelector.end = this.tagReference.getRange().getEndPoint();

		return JsonStream.serialize(tagInstanceLd);
	}

//	public static TagReferenceJsonLd DeserializeFromJsonLd(InputStream inputStream) throws IOException, URISyntaxException {
//		Object jsonObject = JsonUtils.fromInputStream(inputStream);
//		ObjectMapper mapper = new ObjectMapper();
//		JsonNode node = mapper.valueToTree(jsonObject);
//
//		Logger.getLogger("TagReferenceJsonLd").info(JsonUtils.toPrettyString(jsonObject));
//
//		String uri = node.get("target").get("source").asText();
//
//		int startPoint = node.get("target").get("TextPositionSelector").get("start").asInt();
//		int endPoint =  node.get("target").get("TextPositionSelector").get("end").asInt();
//		Range range = new Range(startPoint, endPoint);
//
//		// TagInstance tagInstance, String uri, Range range
//		TagReference internalReference = new TagReference(null, uri, range);
//		return new TagReferenceJsonLd(internalReference);
//	}
}

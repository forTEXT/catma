package de.catma.repository.jsonld;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.Version;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.logging.Logger;



public class TagReferenceJsonLd {

	private final Logger logger;
	private TagReference tagReference;

	public TagReferenceJsonLd(){
		this.logger = Logger.getLogger(this.getClass().getName());
	}

	public TagReferenceJsonLd(TagReference tagReference){
		this.logger = Logger.getLogger(this.getClass().getName());

		this.tagReference = tagReference;
	}

	public TagReference getTagReference(){
		return this.tagReference;
	}

	public TagDefinition FindTagDefinitionForTagInstance(String uuid){
		//TODO: replace with proper calls to the Repo/DB
		Version version = new Version();
		return new TagDefinition(1, "CATMA_1234", "FAKE_TAG_DEFINITION", version, null, null);
	}

	public TagInstance FindTagInstanceFromUUID(String uuid){
		//TODO: replace with proper calls to the Repo/DB
		TagDefinition tagDefinition = this.FindTagDefinitionForTagInstance(uuid);

		return new TagInstance(uuid, tagDefinition);
	}

	public TagReferenceJsonLd Deserialize (InputStream inputStream) throws IOException, URISyntaxException {
		JsonIterator iter = JsonIterator.parse(inputStream, 128);

		TagInstanceLd tagInstanceLd = iter.read(TagInstanceLd.class);
		iter.close();

		String tagInstanceUUID = tagInstanceLd.getUuidFromId();
		TagInstance tagInstance = this.FindTagInstanceFromUUID(tagInstanceUUID);

		Range range = new Range(tagInstanceLd.target.TextPositionSelector.start, tagInstanceLd.target.TextPositionSelector.end);
		this.tagReference = new TagReference(tagInstance, tagInstanceLd.target.source, range);

		return this;
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
}

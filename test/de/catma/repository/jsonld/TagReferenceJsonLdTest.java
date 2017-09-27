package de.catma.repository.jsonld;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class TagReferenceJsonLdTest {
	private String TESTJSON = "{\n" +
			"\t\"@context\": \"http://www.w3.org/ns/anno.jsonld\",\n" +
			"\t\"type\": \"Annotation\",\n" +
			"\t\"id\": \"http://catma.de/portal/annotation/CATMA_4711\",\n" +
			"\t\"body\": {\n" +
			"\t\t\"@context\": {\n" +
			"\t\t\t\"myProp1\": \"http://catma.de/portal/tag/CATMA_789456/property/CATMA_554\"\n" +
			"\t\t},\n" +
			"\t\t\"type\": \"Dataset\",\n" +
			"\t\t\"http://catma.de/portal/tag\": \"http://catma.de/portal/tag/CATMA_789456\",\n" +
			"\t\t\"myProp1\": \"myVal\"\n" +
			"\t},\n" +
			"\t\"target\": {\n" +
			"\t\t\"source\": \"http://catma.de/sourcedocument/doc1\",\n" +
			"\t\t\"TextPositionSelector\": {\n" +
			"\t\t\t\"start\": 42,\n" +
			"\t\t\t\"end\": 125\n" +
			"\t\t}\n" +
			"\t}\n" +
			"}";


	@Test
	public void serializeToJsonLd() throws Exception {
	}

	@Test
	public void deserializeFromJsonLd() throws Exception {

		InputStream inputStream = new ByteArrayInputStream(TESTJSON.getBytes(StandardCharsets.UTF_8.name()));

		TagReferenceJsonLd deserialized = TagReferenceJsonLd.DeserializeFromJsonLd(inputStream);

		assertNotNull(deserialized);
	}

}

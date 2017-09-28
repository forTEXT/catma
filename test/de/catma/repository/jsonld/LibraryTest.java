package de.catma.repository.jsonld;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.utils.JsonUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class LibraryTest {
	@Test
	public void jsonLdJavaChecksValidSchema() throws Exception {
		String BADJSON = "{\n" +
				"\t\"type\": \"Annotation\",\n" +
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

		InputStream inputStream = new ByteArrayInputStream(BADJSON.getBytes(StandardCharsets.UTF_8.name()));

		Object jsonObject = JsonUtils.fromInputStream(inputStream);

		Logger.getLogger("LibraryTest").info(JsonUtils.toPrettyString(jsonObject));

	}


}

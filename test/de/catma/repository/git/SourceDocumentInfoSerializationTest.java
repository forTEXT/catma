package de.catma.repository.git;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.EncodingMode;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.DecodingMode;
import com.jsoniter.spi.JsonException;
import de.catma.document.source.*;
import de.catma.repository.git.serialization.JsoniterAlphabeticOrderingExtension;
import de.catma.repository.git.serialization.model_wrappers.GitSourceDocumentInfo;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

import static org.junit.Assert.*;

public class SourceDocumentInfoSerializationTest {
	private final String expectedSerializedRepresentation = "" +
			"{\n" +
			"\t\"gitContentInfoSet\":{\n" +
			"\t\t\"author\":\"Afrolabs\",\n" +
			"\t\t\"description\":\"A book about things, what they are and where to find them\",\n" +
			"\t\t\"publisher\":\"Afrolabs\",\n" +
			"\t\t\"title\":\"Things\"\n" +
			"\t},\n" +
			"\t\"gitIndexInfoSet\":{\n" +
			"\t\t\"locale\":\"de\",\n" +
			"\t\t\"unseparableCharacterSequences\":[\"a la\"],\n" +
			"\t\t\"userDefinedSeparatingCharacters\":[124]\n" +
			"\t},\n" +
			"\t\"gitTechInfoSet\":{\n" +
			"\t\t\"charset\":\"UTF-8\",\n" +
			"\t\t\"checksum\":123456789,\n" +
			"\t\t\"fileName\":null,\n" +
			"\t\t\"fileOSType\":\"INDEPENDENT\",\n" +
			"\t\t\"fileType\":\"TEXT\",\n" +
			"\t\t\"mimeType\":null,\n" +
			"\t\t\"uRI\":null,\n" +
			"\t\t\"xsltDocumentLocalUri\":\"fake.xslt\"\n" +
			"\t}\n" +
			"}";

	@Test
	public void testSerialization() {
		IndexInfoSet indexInfoSet = new IndexInfoSet(
			Arrays.asList("a la"),
			Arrays.asList('|'),
			Locale.GERMAN
		);

		ContentInfoSet contentInfoSet = new ContentInfoSet(
			"Afrolabs",
			"A book about things, what they are and where to find them",
			"Afrolabs",
			"Things"
		);

		TechInfoSet techInfoSet = new TechInfoSet(
			FileType.TEXT,
			Charset.defaultCharset(),
			FileOSType.INDEPENDENT,
			123456789L,
			"fake.xslt"
		);

		SourceDocumentInfo inputSourceDocumentInfo = new SourceDocumentInfo(
			indexInfoSet, contentInfoSet, techInfoSet
		);

		GitSourceDocumentInfo inputGitSourceDocumentInfo = new GitSourceDocumentInfo(inputSourceDocumentInfo);

		// handle com.jsoniter.spi.JsonException: JsoniterAlphabeticOrderingExtension.enable can only be called once
		try {
			JsoniterAlphabeticOrderingExtension.enable();
		}
		catch (JsonException e) {}
		JsonStream.setMode(EncodingMode.DYNAMIC_MODE);
		String serialized = JsonStream.serialize(inputGitSourceDocumentInfo);

		assert this.expectedSerializedRepresentation.replaceAll("[\n\t]", "").equals(serialized);

		JsonIterator.setMode(DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_STRICTLY);
		GitSourceDocumentInfo outputGitSourceDocumentInfo = JsonIterator.deserialize(
			serialized, GitSourceDocumentInfo.class
		);
		SourceDocumentInfo outputSourceDocumentInfo = outputGitSourceDocumentInfo.getSourceDocumentInfo();

		// assert IndexInfoSet
		assertEquals(indexInfoSet.getUnseparableCharacterSequences(),
				     outputSourceDocumentInfo.getIndexInfoSet().getUnseparableCharacterSequences());
		assertEquals(indexInfoSet.getUserDefinedSeparatingCharacters(),
				     outputSourceDocumentInfo.getIndexInfoSet().getUserDefinedSeparatingCharacters());
		assertEquals(indexInfoSet.getLocale(),
				     outputSourceDocumentInfo.getIndexInfoSet().getLocale());

		// assert ContentInfoSet
		assertEquals(contentInfoSet.getAuthor(), outputSourceDocumentInfo.getContentInfoSet().getAuthor());
		assertEquals(contentInfoSet.getDescription(), outputSourceDocumentInfo.getContentInfoSet().getDescription());
		assertEquals(contentInfoSet.getPublisher(), outputSourceDocumentInfo.getContentInfoSet().getPublisher());
		assertEquals(contentInfoSet.getTitle(), outputSourceDocumentInfo.getContentInfoSet().getTitle());

		// assert TechInfoSet
		assertEquals(techInfoSet.getFileName(), outputSourceDocumentInfo.getTechInfoSet().getFileName());
		assertEquals(techInfoSet.getFileType(), outputSourceDocumentInfo.getTechInfoSet().getFileType());
		assertEquals(techInfoSet.getCharset(), outputSourceDocumentInfo.getTechInfoSet().getCharset());
		assertEquals(techInfoSet.getFileOSType(), outputSourceDocumentInfo.getTechInfoSet().getFileOSType());
		assertEquals(techInfoSet.getChecksum(), outputSourceDocumentInfo.getTechInfoSet().getChecksum());
		assertEquals(techInfoSet.getMimeType(), outputSourceDocumentInfo.getTechInfoSet().getMimeType());
		assertEquals(techInfoSet.getURI(), outputSourceDocumentInfo.getTechInfoSet().getURI());
		assertEquals(techInfoSet.getXsltDocumentLocalUri(),
				     outputSourceDocumentInfo.getTechInfoSet().getXsltDocumentLocalUri());
	}
}

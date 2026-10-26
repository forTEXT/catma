package de.catma.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.catma.serialization.tei.TeiDocument;
import de.catma.serialization.tei.TeiDocumentFactory;
import nu.xom.Document;
import nu.xom.ParsingException;

class SafeXmlBuilderTest {
	@TempDir
	Path tempDir;

	private static Document build(String xml) throws Exception {
		return SafeXmlBuilder.build(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void doesNotLoadExternalEntities() throws Exception {
		Path secret = tempDir.resolve("secret.txt");
		Files.writeString(secret, "secret-value");

		Document document = build(
				"<!DOCTYPE d [<!ENTITY x SYSTEM \"" + secret.toUri() + "\"><!ENTITY i \"internal\">]><d>&x;|&i;</d>"
		);

		assertEquals("|internal", document.getRootElement().getValue());
	}

	@Test
	void acceptsDoctypeButDoesNotLoadExternalDtd() throws Exception {
		Path dtd = tempDir.resolve("defaults.dtd");
		Files.writeString(dtd, "<!ATTLIST p lang CDATA \"en\">");

		Document document = build("<!DOCTYPE p SYSTEM \"" + dtd.toUri() + "\"><p>text</p>");

		assertEquals("text", document.getRootElement().getValue());
		assertNull(document.getRootElement().getAttributeValue("lang"));
	}

	@Test
	void doesNotFetchRemoteDtd() throws Exception {
		// 192.0.2.0/24 is reserved for documentation, so a fetch would fail rather than succeed
		Document document = build("<!DOCTYPE p SYSTEM \"http://192.0.2.1/unreachable.dtd\"><p>text</p>");

		assertEquals("text", document.getRootElement().getValue());
	}

	@Test
	void explainsEntitiesThatOnlyAnExternalDtdDefines() throws Exception {
		Path dtd = tempDir.resolve("entities.dtd");
		Files.writeString(dtd, "<!ENTITY eacute \"&#233;\">");

		ParsingException e = assertThrows(ParsingException.class, () -> build("<!DOCTYPE p SYSTEM \"" + dtd.toUri() + "\"><p>caf&eacute;</p>"));

		assertTrue(e.getMessage().contains("eacute"), e.getMessage());
		assertTrue(e.getMessage().contains("which CATMA doesn't load"), e.getMessage());
	}

	@Test
	void leavesOtherParsingErrorsAlone() {
		ParsingException e = assertThrows(ParsingException.class, () -> build("<!DOCTYPE p SYSTEM \"unused.dtd\"><p><a></p>"));

		assertFalse(e.getMessage().contains("which CATMA doesn't load"), e.getMessage());
	}

	@Test
	void parsesTeiWithCustomNodeFactory() throws Exception {
		TeiDocument teiDocument = new TeiDocumentFactory().createEmptyDocument("test");

		// the header is looked up as a TeiElement, which only exists if the TeiNodeFactory was used
		assertNotNull(teiDocument.getTeiHeader());
	}
}

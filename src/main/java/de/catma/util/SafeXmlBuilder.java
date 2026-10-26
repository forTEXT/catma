package de.catma.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.NodeFactory;
import nu.xom.ParsingException;

/**
 * Parses XML into XOM documents without loading anything the document refers to externally.
 * <p>
 * A plain <code>new Builder()</code> resolves external entities and DTDs, so an uploaded or imported document could have the server read local
 * files or request URLs, and show the result back to the user. XOM switches the SAX features that would turn this off back on, so instead every
 * external entity and DTD is resolved to nothing. DOCTYPE declarations and internal entities still work; entities defined only in an external DTD
 * (e.g. <code>&amp;eacute;</code> from an XHTML or older TEI DTD) can't be resolved, and the resulting error says why.
 * <p>
 * The parser is the JDK's own, which XOM recognises and trusts, rather than the Xerces that XOM brings along.
 */
public final class SafeXmlBuilder {
	private SafeXmlBuilder() {
	}

	public static Document build(InputStream inputStream) throws ParsingException, IOException {
		return build(inputStream, null);
	}

	public static Document build(InputStream inputStream, NodeFactory factory) throws ParsingException, IOException {
		List<String> ignoredExternalReferences = new ArrayList<>();

		XMLReader xmlReader;
		try {
			xmlReader = SAXParserFactory.newDefaultInstance().newSAXParser().getXMLReader();
		}
		catch (ParserConfigurationException | SAXException e) {
			throw new IOException("Failed to create an XML parser", e);
		}
		xmlReader.setEntityResolver((publicId, systemId) -> {
			ignoredExternalReferences.add(systemId);
			return new InputSource(new StringReader(""));
		});

		try {
			return new Builder(xmlReader, false, factory).build(inputStream);
		}
		catch (ParsingException e) {
			if (ignoredExternalReferences.isEmpty() || e.getMessage() == null || !e.getMessage().toLowerCase(Locale.ROOT).contains("entity")) {
				throw e;
			}

			throw new ParsingException(
					String.format(
							"%s. The document refers to an external DTD or entity (%s), which CATMA doesn't load. If it uses entities defined there, "
							+ "such as named character references like &eacute;, replace them with the characters themselves or with numeric "
							+ "references like &#233;.",
							e.getMessage(),
							String.join(", ", ignoredExternalReferences)
					),
					e.getURI(),
					e.getLineNumber(),
					e.getColumnNumber(),
					e
			);
		}
	}
}

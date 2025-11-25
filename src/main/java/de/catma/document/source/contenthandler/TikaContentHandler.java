package de.catma.document.source.contenthandler;

import de.catma.document.source.FileType;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The content handler that initially handles all uploaded files that are not XML.
 * <p>
 * Uses Tika for parsing.
 *
 * @see de.catma.document.source.TechInfoSet
 */
public class TikaContentHandler extends AbstractSourceContentHandler {
	private final Tika tika;

	public TikaContentHandler(Tika tika) {
		this.tika = tika;
	}

	private void load(InputStream inputStream) throws IOException {
		MediaType mediaType = MediaType.parse(getSourceDocumentInfo().getTechInfoSet().getMimeType());
		Metadata metadata = new Metadata();
		if (mediaType.getBaseType().toString().equals(FileType.TEXT.getMimeType())) {
			metadata.set(Metadata.CONTENT_TYPE, new MediaType(mediaType, getSourceDocumentInfo().getTechInfoSet().getCharset()).toString());
		}

		try {
			setContent(
					// some texts seem to include invalid unicode characters and this causes problems when converting text to HTML for GUI delivery and during
					// indexing
					tika.parseToString(inputStream, metadata, -1) // -1 is important, otherwise the default is 100k chars!
							.replaceAll("[^\\x09\\x0A\\x0D\\x20-\\uD7FF\\uE000-\\uFFFD\\u10000-\\u10FFFF]", "?")
			);
		}
		catch (Exception e) {
			throw new IOException(e);
		} 
	}

	@Override
	public void load() throws IOException {
		try (BufferedInputStream bufferedInputStream = new BufferedInputStream(getSourceDocumentInfo().getTechInfoSet().getURI().toURL().openStream())) {
			load(bufferedInputStream);
		}
	}
}

package de.catma.document.source.contenthandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import de.catma.document.source.FileType;

public class TikaContentHandler extends AbstractSourceContentHandler {

	@Override
	public void load(InputStream is) throws IOException {
		Tika tika = new Tika();
		tika.setMaxStringLength(-1);
		
		Metadata metadata = new Metadata();
		MediaType type = MediaType.parse(getSourceDocumentInfo().getTechInfoSet().getMimeType());
		
		if (type.getBaseType().toString().equals(FileType.TEXT.getMimeType())) {
			metadata.set(
				Metadata.CONTENT_TYPE, 
				new MediaType(type, getSourceDocumentInfo().getTechInfoSet().getCharset()).toString());
		}

		try {
	        // some texts seem to include non valid unicode characters
	        // and this causes problems when converting text to HTML
	        // for GUI delivery and during indexing 
//			setContent(
//					tika.parseToString(is, metadata));
			setContent(
				tika.parseToString(is, metadata).replaceAll(
					"[^\\x09\\x0A\\x0D\\x20-\\uD7FF\\uE000-\\uFFFD\\u10000-\\u10FFFF]", "?"));
		} catch (TikaException e) {
			throw new IOException(e);
		} 
	}

	@Override
	public void load() throws IOException {
        BufferedInputStream bis = null;
        try {
        	
            bis = new BufferedInputStream(
            		getSourceDocumentInfo().getTechInfoSet().getURI().toURL().openStream());

            load(bis);
        }
        finally {
            if (bis != null) {
				bis.close();
            }
        }
	}

}

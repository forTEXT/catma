package de.catma.api.pre.serialization.model_wrappers;

import de.catma.document.source.SourceDocument;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class PreApiSourceDocument {
    private String id;
    private String bodyUrl;
    private String crc32bChecksum;
    private int size;
    private String title;

    private static final Logger logger = Logger.getLogger(PreApiSourceDocument.class.getName());

    public PreApiSourceDocument() {
    }

    public PreApiSourceDocument(SourceDocument sourceDocument, String bodyUrl) {
        id = sourceDocument.getUuid();
        this.bodyUrl = bodyUrl;
        title = sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet().getTitle();

        try {
            byte[] bytes = sourceDocument.getContent().getBytes(StandardCharsets.UTF_8);

            // checksum - not using sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getTechInfoSet().getChecksum() because it does not
            // respect the charset when it is created
            CRC32 crc = new CRC32();
            crc.update(bytes);
            crc32bChecksum = Long.toHexString(crc.getValue());

            // size
            size = bytes.length;
        }
        catch (IOException e) {
            logger.log(Level.WARNING, "Couldn't get document content", e);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBodyUrl() {
        return bodyUrl;
    }

    public void setBodyUrl(String bodyUrl) {
        this.bodyUrl = bodyUrl;
    }

    public String getCrc32bChecksum() {
        return crc32bChecksum;
    }

    public void setCrc32bChecksum(String crc32bChecksum) {
        this.crc32bChecksum = crc32bChecksum;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

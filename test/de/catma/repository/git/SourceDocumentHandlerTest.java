package de.catma.repository.git;

import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;
import de.catma.document.source.contenthandler.StandardContentHandler;
import de.catma.util.IDGenerator;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Properties;

import static org.junit.Assert.*;

public class SourceDocumentHandlerTest {
	private Properties catmaProperties;

    public SourceDocumentHandlerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));
    }

    @Test
    public void insert() throws Exception {
    	File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
        File convertedSourceDocument = new File("testdocs/rose_for_emily.txt");

		byte[] originalSourceDocumentBytes = Files.readAllBytes(originalSourceDocument.toPath());

        TechInfoSet techInfoSet = new TechInfoSet(convertedSourceDocument.getName(),
			Files.probeContentType(convertedSourceDocument.toPath()),
			convertedSourceDocument.toURI()
		);
        techInfoSet.setCharset(Charset.forName("UTF-8"));

        StandardContentHandler standardContentHandler = new StandardContentHandler();
        standardContentHandler.setSourceDocumentInfo(
			new SourceDocumentInfo(null, null, techInfoSet)
		);

        FileInputStream fileInputStream = new FileInputStream(convertedSourceDocument);
        standardContentHandler.load(fileInputStream);

		IDGenerator idGenerator = new IDGenerator();
        SourceDocument sourceDocument = new SourceDocument(
			idGenerator.generate(), standardContentHandler
		);

        SourceDocumentHandler sourceDocumentHandler = new SourceDocumentHandler(
			this.catmaProperties
		);
        sourceDocumentHandler.insert(originalSourceDocumentBytes, sourceDocument, null);

        // TODO: assert something
    }
}

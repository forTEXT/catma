package de.catma.repository.git;

import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;
import de.catma.document.source.contenthandler.StandardContentHandler;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.*;

public class SourceDocumentHandlerTest {
    @Test
    public void insert() throws Exception {
        String propertiesFile = System.getProperties().containsKey("prop") ? System.getProperties().getProperty("prop") : "catma.properties";

        Properties catmaProperties = new Properties();
        catmaProperties.load(new FileInputStream(propertiesFile));

        File inputFile = new File("testdocs/rose_for_emily.txt");

        TechInfoSet techInfoSet = new TechInfoSet(inputFile.getName(), Files.probeContentType(inputFile.toPath()), inputFile.toURI());
        techInfoSet.setCharset(Charset.forName("UTF-8"));
        StandardContentHandler standardContentHandler = new StandardContentHandler();
        standardContentHandler.setSourceDocumentInfo(new SourceDocumentInfo(null, null, techInfoSet));

        FileInputStream fileInputStream = new FileInputStream(inputFile);
        standardContentHandler.load(fileInputStream);

        SourceDocument document = new SourceDocument(UUID.randomUUID().toString(), standardContentHandler);

        SourceDocumentHandler sdh = new SourceDocumentHandler(catmaProperties);
        sdh.insert(document);
    }
}

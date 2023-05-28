package de.catma.repository.git;

import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.models.GitMarkupCollectionHeader;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class GitAnnotationCollectionHandlerTest {
	@Test
	void loadSampleAnnotationCollectionHeader() throws IOException {
		File sampleAnnotationCollectionHeaderFile = new File("testdocs/sample_headers/annotation_collection.json");

		String serializedAnnotationCollectionHeader = FileUtils.readFileToString(
				sampleAnnotationCollectionHeaderFile, StandardCharsets.UTF_8
		);

		GitMarkupCollectionHeader gitMarkupCollectionHeader = new SerializationHelper<GitMarkupCollectionHeader>()
				.deserialize(serializedAnnotationCollectionHeader, GitMarkupCollectionHeader.class);

		assert gitMarkupCollectionHeader.getName().equals("Alice in Wonderland Default Annotations");
		assert gitMarkupCollectionHeader.getSourceDocumentId().equals("D_485EFD9F-7DD7-404B-B03E-0631FA3464D4");
	}
}

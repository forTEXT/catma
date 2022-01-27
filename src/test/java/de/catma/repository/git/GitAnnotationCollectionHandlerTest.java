package de.catma.repository.git;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.models.GitMarkupCollectionHeader;

class GitAnnotationCollectionHandlerTest {

	@Test
	void loadCatma641CollectionHeader() throws IOException {
		File catma641AnnotationCollectionHeaderFile = new File("testdocs/header_641.json");
		
		String serializedMarkupCollectionHeaderFile = FileUtils.readFileToString(
				catma641AnnotationCollectionHeaderFile, StandardCharsets.UTF_8
		);

		GitMarkupCollectionHeader markupCollectionHeader = new SerializationHelper<GitMarkupCollectionHeader>()
				.deserialize(serializedMarkupCollectionHeaderFile, GitMarkupCollectionHeader.class);
		
		assert markupCollectionHeader.getName().equals(
				"Alice in Wonderland Default Annotations");
		assert markupCollectionHeader.getSourceDocumentId().equals(
				"D_485EFD9F-7DD7-404B-B03E-0631FA3464D4");
	}

}

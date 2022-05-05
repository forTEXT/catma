package de.catma.ui.module.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.UI;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.corpus.Corpus;
import de.catma.document.corpus.CorpusExporter;
import de.catma.document.source.SourceDocumentReference;
import de.catma.project.Project;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.util.IDGenerator;

public class CollectionXMLExportStreamSource implements StreamSource {

	private Supplier<Collection<SourceDocumentReference>> documentSupplier;
	private Supplier<Collection<AnnotationCollectionReference>> collectionReferenceSupplier;
	private Supplier<Project> projectSupplier;

	public CollectionXMLExportStreamSource(
			Supplier<Collection<SourceDocumentReference>> documentSupplier,
			Supplier<Collection<AnnotationCollectionReference>> collectionReferenceSupplier,
			Supplier<Project> projectSupplier) {
		this.documentSupplier = documentSupplier;
		this.collectionReferenceSupplier = collectionReferenceSupplier;
		this.projectSupplier = projectSupplier;
	}

	@Override
	public InputStream getStream() {
		final UI ui = UI.getCurrent();
		final Project project = projectSupplier.get();
		final Corpus corpus = new Corpus();
		final Collection<SourceDocumentReference> documents = documentSupplier.get();
		final Collection<AnnotationCollectionReference> collectionReferences = collectionReferenceSupplier.get();
		try {
		
			Set<String> documentIds = documents.stream().map(doc -> doc.getUuid()).collect(Collectors.toSet());
			
			collectionReferences.stream()
				.forEach(ref -> documentIds.add(ref.getSourceDocumentId()));
	
			for (String documentId : documentIds) {
				corpus.addSourceDocument(project.getSourceDocumentReference(documentId));
			}
			
			if (corpus.getSourceDocuments().size() == 0) {
				return null;
			}
		
			collectionReferences.forEach(ref -> corpus.addUserMarkupCollectionReference(ref));
			
			
			File tempFile = File.createTempFile(new IDGenerator().generate() + "_AnnotationCollection_Export", "tgz");
			try (FileOutputStream fos = new FileOutputStream(tempFile)) {
				new CorpusExporter(project, true).export(project.getName(), corpus, fos);
			}
	
	        return new FileInputStream(tempFile);
		} catch (Exception e) {
			((ErrorHandler)ui).showAndLogError("Error exporting Documents and Collections!", e);
		}		

		return null;
	}

}

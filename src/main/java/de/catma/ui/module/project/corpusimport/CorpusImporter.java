package de.catma.ui.module.project.corpusimport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import com.google.common.collect.Lists;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.FileType;
import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;
import de.catma.document.source.contenthandler.AbstractSourceContentHandler;
import de.catma.document.source.contenthandler.OldXMLContentHandler;
import de.catma.document.source.contenthandler.StandardContentHandler;
import de.catma.document.source.contenthandler.XML2ContentHandler;
import de.catma.project.Project;
import de.catma.serialization.TagsetDefinitionImportStatus;
import de.catma.serialization.intrinsic.xml.XmlMarkupCollectionSerializationHandler;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.module.project.ProjectView;
import de.catma.ui.module.project.documentwizard.UploadFile;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

@SuppressWarnings("deprecation")
public class CorpusImporter {

	public Void importCorpus(
		final ProgressListener progressListener, 
		final File corpusFile, 
		final List<CorpusImportDocumentMetadata> documentMetadataList,
		final String tempDir,
		final UI ui,
		final Project project) throws Exception {
		progressListener.setProgress("Importing Corpus");
		
		GZIPInputStream gzipIs = new GZIPInputStream(new FileInputStream(corpusFile));
		
		try (TarArchiveInputStream taIs = new TarArchiveInputStream(gzipIs)) {
			TarArchiveEntry entry = taIs.getNextTarEntry();
			while (entry != null) {
				final String entryName = entry.getName();
				final String[] pathParts = entry.getName().split(Pattern.quote("/"));
				
				final String documentIdPart = pathParts[2];
				final String documentId = documentIdPart.substring(documentIdPart.indexOf("__")+3);
				final String idUri = "catma://"+documentId;
				
				final CorpusImportDocumentMetadata documentMetadata = 
					documentMetadataList.stream().filter(metadata -> metadata.getSourceDocID().equals(idUri)).findFirst().orElse(null);
				
				final Locale locale = Locale.forLanguageTag(documentMetadata.getSourceDocLocale());
				
				final boolean useApostrophe = 
					Arrays.asList(documentMetadata.getSourceDocSepChars()).contains(String.valueOf(UploadFile.APOSTROPHE));
				
				if (pathParts[3].equals("annotationcollections")) {
					
					progressListener.setProgress("Importing Collection %1$s", pathParts[4]);
					
					ui.accessSynchronously(() -> {
						try {
							final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
							IOUtils.copy(taIs, buffer);
							
							SourceDocument document = project.getSourceDocument(documentId);
							Pair<AnnotationCollection, List<TagsetDefinitionImportStatus>> loadResult =
									project.loadAnnotationCollection(new ByteArrayInputStream(buffer.toByteArray()), document);
							
							List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatusList = loadResult.getSecond();
							final AnnotationCollection annotationCollection = loadResult.getFirst();
							
							Optional<TagsetDefinition> optIntrinsicTagset = 
								annotationCollection.getTagLibrary().getTagsetDefinitions().stream()
								.filter(tagsetDef -> tagsetDef.getName().equals("Intrinsic Markup"))
								.findFirst();
							if (optIntrinsicTagset.isPresent()) {
								TagsetDefinition intrinsicTagset =
									optIntrinsicTagset.get();
								
								List<TagReference> intrinsicAnnotations = 
									annotationCollection.getTagReferences(intrinsicTagset);
								if (!intrinsicAnnotations.isEmpty()) {
									annotationCollection.removeTagReferences(intrinsicAnnotations);
								}
								annotationCollection.getTagLibrary().remove(intrinsicTagset);
							}
							if (!annotationCollection.isEmpty()) {
								project.importCollection(
										tagsetDefinitionImportStatusList, annotationCollection);
							}
						}
						catch (Exception e) {
			    			Logger.getLogger(ProjectView.class.getName()).log(
			    					Level.SEVERE, 
			    					"Error importing the CATMA 5 Corpus: " + entryName, 
			    					e);
			    			String errorMsg = e.getMessage();
			    			if ((errorMsg == null) || (errorMsg.trim().isEmpty())) {
			    				errorMsg = "";
			    			}

			    			Notification.show(
			    				"Error", 
			    				String.format(
			    						"Error importing the CATMA 5 Corpus! "
			    						+ "This Collection will be skipped!\n The underlying error message was:\n%1$s", 
			    						errorMsg), 
			    				Type.ERROR_MESSAGE);										}
					});
				}
				else {
					final String title = 
						(documentMetadata.getSourceDocName()==null|| documentMetadata.getSourceDocName().isEmpty())?
								documentId
								:documentMetadata.getSourceDocName();
					
					progressListener.setProgress("Importing Document %1$s", title);
					
					final File tempFile = new File(new File(tempDir), documentId);
					
					if (tempFile.exists()) {
						tempFile.delete();
					}
					try (FileOutputStream fos = new FileOutputStream(tempFile)) {
						IOUtils.copy(taIs, fos);
					}

					ui.accessSynchronously(() -> {
						IDGenerator idGenerator = new IDGenerator();
						IndexInfoSet indexInfoSet =  new IndexInfoSet(
								Collections.emptyList(), 
								useApostrophe?Lists.newArrayList(UploadFile.APOSTROPHE):Collections.emptyList(), 
								locale);
						TechInfoSet techInfoSet = 
							new TechInfoSet(documentId, FileType.TEXT.getMimeType(), tempFile.toURI());
						
						ContentInfoSet contentInfoSet =
								new ContentInfoSet(
									documentMetadata.getSourceDocAuthor(), 
									documentMetadata.getSourceDocDescription(), 
									documentMetadata.getSourceDocPublisher(),
									title);
						
						techInfoSet.setCharset(Charset.forName("UTF-8"));
						SourceDocumentInfo documentInfo = 
								new SourceDocumentInfo(indexInfoSet, contentInfoSet, techInfoSet);

						AbstractSourceContentHandler handler = null;
						boolean loadIntrinsicMarkup = false;
						if (entryName.endsWith("xml2")) {
							handler = new XML2ContentHandler();
							loadIntrinsicMarkup = true;
						}
						else if (entryName.endsWith("xml")) {
							handler = new OldXMLContentHandler();
							loadIntrinsicMarkup= true;
						}
						else {
							handler = new StandardContentHandler();
						}
						
						handler.setSourceDocumentInfo(documentInfo);
						
						SourceDocument document = new SourceDocument(documentId, handler);
						try {
							project.insert(document);
							
							
							if (loadIntrinsicMarkup) {
								final TagManager tagmanager = new TagManager(new TagLibrary());
								
								XmlMarkupCollectionSerializationHandler markupHandler =
										new XmlMarkupCollectionSerializationHandler(
												tagmanager, (XML2ContentHandler)handler, 
												project.getUser().getIdentifier());
								try (FileInputStream fis = new FileInputStream(tempFile)) {
									AnnotationCollection collection = 
										markupHandler.deserialize(document, idGenerator.generateCollectionId(), fis);
									
								}
							}
							
						}
						catch (Exception e) {
			    			Logger.getLogger(ProjectView.class.getName()).log(
			    					Level.SEVERE, 
			    					"Error importing the CATMA 5 Corpus: " + entryName, 
			    					e);
			    			String errorMsg = e.getMessage();
			    			if ((errorMsg == null) || (errorMsg.trim().isEmpty())) {
			    				errorMsg = "";
			    			}

			    			Notification.show(
			    				"Error", 
			    				String.format(
			    						"Error importing the CATMA 5 Corpus! "
			    						+ "This Document will be skipped!\n The underlying error message was:\n%1$s", 
			    						errorMsg), 
			    				Type.ERROR_MESSAGE);		
			    		}
					});
				}
			
				entry = taIs.getNextTarEntry();
			}
		
		}					
		return null;
	}

}

package de.catma.heureclea;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import javax.naming.InitialContext;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import de.catma.backgroundservice.DefaultBackgroundServiceProvider;
import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryManager;
import de.catma.document.repository.RepositoryPropertiesName;
import de.catma.document.repository.RepositoryReference;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.contenthandler.SourceContentHandler;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.serialization.tei.TeiUserMarkupCollectionSerializationHandler;
import de.catma.tag.TagManager;

public class HeurecleaExporter {
	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyMMddhhmm");
	
	private Logger logger = Logger.getLogger(HeurecleaExporter.class.getName());

	public HeurecleaExporter() {
	}
	
	public void export() throws IOException {
		try {
			Properties properties = 
					(Properties) new InitialContext().lookup(
							RepositoryPropertiesName.CATMAPROPERTIES.name());
			
			String exportFolder = properties.getProperty("heurecleaExportFolder");
			
			TagManager tagManager = new TagManager();
			RepositoryManager repoManager = new RepositoryManager(
					new DefaultBackgroundServiceProvider(), 
					tagManager, properties);
			if (!repoManager.getRepositoryReferences().isEmpty()) {
				RepositoryReference repoRef = 
					repoManager.getRepositoryReferences().iterator().next();
				
				Map<String,String> userIdentification = 
						new HashMap<String, String>(1);
				userIdentification.put("user.ident", "heureclea");
				
				Repository repo = repoManager.openRepository(repoRef, userIdentification);

				Collection<Corpus> corpora = repo.getCorpora();
				
				String date = FORMATTER.format(new Date());
				String fileName = "heureclea_" + date + ".tar.gz";
				
				OutputStream tarFileOs = 
					new GZIPOutputStream(
						new FileOutputStream(new File(exportFolder, fileName)));
				
				TarArchiveOutputStream taOut = new TarArchiveOutputStream(tarFileOs, "UTF-8");
				try {
					
					taOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
					taOut.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
					
					for (Corpus corpus : corpora) {
						String corpusName = cleanupName(corpus.toString());
						logger.info("exporting corpus " +  corpus);
						
						for (SourceDocument sd : corpus.getSourceDocuments()) {
							logger.info("exporting Source Document " + sd);
							
							TarArchiveEntry sdEntry = 
								new TarArchiveEntry(
									"heureclea_" 
										+ date 
										+ "/" 
										+ corpusName 
										+ "/" 
										+ cleanupName(sd.getID()) 
										+ "/" 
										+ cleanupName(getFilename(sd)));
						
							byte[] sdContent = 
								sd.getContent().getBytes(Charset.forName("UTF8"));
							
							sdEntry.setSize(sdContent.length);
							
							taOut.putArchiveEntry(sdEntry);
							
							taOut.write(sdContent);
							
							taOut.closeArchiveEntry();
							
							for (UserMarkupCollectionReference umcRef 
									: sd.getUserMarkupCollectionRefs()) {
								logger.info("exporting User Markup Collection " +  umcRef);
								
								UserMarkupCollection umc = 
										repo.getUserMarkupCollection(umcRef);

								TeiUserMarkupCollectionSerializationHandler handler =
										new TeiUserMarkupCollectionSerializationHandler(
												repo.getTagManager(), false);
								ByteArrayOutputStream teiDocOut = new ByteArrayOutputStream();
								handler.serialize(
									repo.getUserMarkupCollection(umcRef), sd, teiDocOut);

								byte[] umcContent = teiDocOut.toByteArray();
								
								String umcEntryName = 
										"heureclea_" 
											+ date 
											+ "/"
											+ corpusName 										 
											+ "/" + cleanupName(sd.getID()) 
											+ "/usermarkupcollections/" 
											+ cleanupName(umc.getName())
											+ ".xml";
								
								TarArchiveEntry umcEntry = 
									new TarArchiveEntry(umcEntryName);
								
								umcEntry.setSize(umcContent.length);
								
								taOut.putArchiveEntry(umcEntry);
								taOut.write(umcContent);
								
								taOut.closeArchiveEntry();
							}
							
						}
					}
				}
				finally {
					taOut.finish();
					taOut.close();
					logger.info("finished heureclea export");
				}
			}
			else {
				logger.warning("no repository configured!");
			}
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	private String cleanupName(String name) {
		return name.replaceAll("[/:]|\\s", "_");
	}
	
	private String getFilename(SourceDocument sourceDocument) {
		SourceContentHandler sourceContentHandler = 
				sourceDocument.getSourceContentHandler();
		String title = 
				sourceContentHandler.getSourceDocumentInfo()
					.getContentInfoSet().getTitle();

		return sourceDocument.getID() 
			+ (((title==null)||title.isEmpty())?"":("_"+title)) +
			".txt";
	};

}
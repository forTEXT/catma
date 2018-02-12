package de.catma.document.corpus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.contenthandler.SourceContentHandler;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.serialization.tei.TeiUserMarkupCollectionSerializationHandler;

public class CorpusExporter {
	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyMMddhhmm");

	private Repository repo;

	private String date;

	private boolean simpleEntryStyle;
	
	public CorpusExporter(Repository repo, boolean simpleEntryStyle) {
		this.repo = repo;
		this.simpleEntryStyle = simpleEntryStyle;
		this.date = FORMATTER.format(new Date());
	}

	public void export(
		String exportName, Collection<Corpus> corpora,  OutputStream os) throws IOException {
		
		OutputStream tarFileOs = new GZIPOutputStream(os);
		
		TarArchiveOutputStream taOut = new TarArchiveOutputStream(tarFileOs, "UTF-8");
		try {
			
			taOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
			taOut.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
			
			for (Corpus corpus : corpora) {
				String corpusName = cleanupName(corpus.toString());
				
				for (SourceDocument sd : corpus.getSourceDocuments()) {
					
					TarArchiveEntry sdEntry = 
						new TarArchiveEntry(getSourceDocEntryName(exportName, corpusName, sd));
				
					byte[] sdContent = 
						sd.getContent().getBytes(Charset.forName("UTF8"));
					
					sdEntry.setSize(sdContent.length);
					
					taOut.putArchiveEntry(sdEntry);
					
					taOut.write(sdContent);
					
					taOut.closeArchiveEntry();
					
					for (UserMarkupCollectionReference umcRef 
							: corpus.getUserMarkupCollectionRefs(sd)) {
						
						UserMarkupCollection umc = 
								repo.getUserMarkupCollection(umcRef);

						TeiUserMarkupCollectionSerializationHandler handler =
								new TeiUserMarkupCollectionSerializationHandler(
										repo.getTagManager(), false);
						ByteArrayOutputStream teiDocOut = new ByteArrayOutputStream();
						handler.serialize(
							repo.getUserMarkupCollection(umcRef), sd, teiDocOut);

						byte[] umcContent = teiDocOut.toByteArray();
						
						String umcEntryName = getUmcEntryName(exportName, corpusName, umc, sd);
						
						TarArchiveEntry umcEntry = 
							new TarArchiveEntry(umcEntryName);
						
						umcEntry.setSize(umcContent.length);
						
						taOut.putArchiveEntry(umcEntry);
						taOut.write(umcContent);
						
						taOut.closeArchiveEntry();
					}
					
					sd.unload();
					
				}
			}
		}
		finally {
			taOut.finish();
			taOut.close();
		}
		
	}
	
	private String getUmcEntryName(String exportName, String corpusName, UserMarkupCollection umc, SourceDocument sd) {
		if (simpleEntryStyle) {
			return corpusName 
					+ "/" 
					+ cleanupName(getFilename(sd))
					+ "/annotationcollections/" 
					+ cleanupName(umc.getName())
					+ ".xml";
		}
		
		return exportName 
				+ "_" 
				+ date 
				+ "/"
				+ corpusName 										 
				+ "/" + cleanupName(sd.getID()) 
				+ "/annotationcollections/" 
				+ cleanupName(umc.getName())
				+ ".xml";

	}

	private String getSourceDocEntryName(String exportName, String corpusName, SourceDocument sd) {
		if (simpleEntryStyle) {
			String cleanFilename = cleanupName(getFilename(sd));
			return corpusName 
					+ "/" 
					+ cleanFilename 
					+ "/" 
					+ cleanFilename; 
		}
		return exportName 
				+ "_" 
				+ date 
				+ "/" 
				+ corpusName 
				+ "/" 
				+ cleanupName(sd.getID()) 
				+ "/" 
				+ cleanupName(getFilename(sd));
	}

	public String getDate() {
		return date;
	}
	
	public String cleanupName(String name) {
		return name.replaceAll("[/:]|\\s", "_");
	}
	
	private String getFilename(SourceDocument sourceDocument) {
		SourceContentHandler sourceContentHandler = 
				sourceDocument.getSourceContentHandler();
		String title = 
				sourceContentHandler.getSourceDocumentInfo()
					.getContentInfoSet().getTitle();
		if (simpleEntryStyle) {
			return sourceDocument.toString() + ".txt";
		}
		return sourceDocument.getID() 
			+ (((title==null)||title.isEmpty())?"":("_"+title)) +
			".txt";
	};

}

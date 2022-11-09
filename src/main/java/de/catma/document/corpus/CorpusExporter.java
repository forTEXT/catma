package de.catma.document.corpus;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentReference;
import de.catma.project.Project;
import de.catma.serialization.tei.TeiUserMarkupCollectionSerializationHandler;

public class CorpusExporter {
	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyMMddhhmm");

	private Project project;

	private String date;

	private boolean simpleEntryStyle;
	
	public CorpusExporter(Project project, boolean simpleEntryStyle) {
		this.project = project;
		this.simpleEntryStyle = simpleEntryStyle;
		this.date = FORMATTER.format(new Date());
	}

	public void export(
		String exportName, Corpus corpus,  OutputStream os) throws Exception {
		
		OutputStream tarFileOs = new GZIPOutputStream(os);
		
		TarArchiveOutputStream taOut = new TarArchiveOutputStream(tarFileOs, "UTF-8");
		try {
			
			taOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
			taOut.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
			
			for (SourceDocumentReference sdRef : corpus.getSourceDocuments()) {
				
				TarArchiveEntry sdEntry = 
					new TarArchiveEntry(getSourceDocEntryName(exportName, sdRef));
				SourceDocument sd = project.getSourceDocument(sdRef.getUuid());
				
				byte[] sdContent = 
					sd.getContent().getBytes(Charset.forName("UTF8"));
				
				sdEntry.setSize(sdContent.length);
				
				taOut.putArchiveEntry(sdEntry);
				
				taOut.write(sdContent);
				
				taOut.closeArchiveEntry();
				
				for (AnnotationCollectionReference umcRef 
						: corpus.getUserMarkupCollectionRefs(sdRef)) {
					
					AnnotationCollection umc = 
							project.getAnnotationCollection(umcRef);

					TeiUserMarkupCollectionSerializationHandler handler =
							new TeiUserMarkupCollectionSerializationHandler(
									project.getTagManager(), 
									project.getVersion(), 
									false);
					ByteArrayOutputStream teiDocOut = new ByteArrayOutputStream();
					handler.serialize(
						project.getAnnotationCollection(umcRef), sd, teiDocOut);

					byte[] umcContent = teiDocOut.toByteArray();
					
					String umcEntryName = getUmcEntryName(exportName, umc, sdRef);
					
					TarArchiveEntry umcEntry = 
						new TarArchiveEntry(umcEntryName);
					
					umcEntry.setSize(umcContent.length);
					
					taOut.putArchiveEntry(umcEntry);
					taOut.write(umcContent);
					
					taOut.closeArchiveEntry();
				}
			}
		}
		finally {
			taOut.finish();
			taOut.close();
		}
		
	}
	
	private String getUmcEntryName(String exportName, AnnotationCollection umc, SourceDocumentReference sd) {
		if (simpleEntryStyle) {
			return cleanupName(getFilename(sd, false))
					+ "/annotationcollections/" 
					+ cleanupName(umc.getName())
					+ ".xml";
		}
		
		return exportName 
				+ "_" 
				+ date 
				+ "/" + cleanupName(sd.getUuid()) 
				+ "/annotationcollections/" 
				+ cleanupName(umc.getName())
				+ ".xml";

	}

	private String getSourceDocEntryName(String exportName, SourceDocumentReference sd) {
		if (simpleEntryStyle) {
			return cleanupName(getFilename(sd, false)) 
					+ "/" 
					+ cleanupName(getFilename(sd, true)); 
		}
		return exportName 
				+ "_" 
				+ date 
				+ "/" 
				+ cleanupName(sd.getUuid()) 
				+ "/" 
				+ cleanupName(getFilename(sd, true));
	}

	public String getDate() {
		return date;
	}
	
	public String cleanupName(String name) {
		return name.replaceAll("[/:]|\\s", "_");
	}
	
	private String getFilename(SourceDocumentReference sourceDocumentReference, boolean withFileExtension) {

		String title = 
				sourceDocumentReference.getSourceDocumentInfo()
					.getContentInfoSet().getTitle();
		if (simpleEntryStyle) {
			return sourceDocumentReference.toString() + (withFileExtension?".txt":"");
		}
		return sourceDocumentReference.getUuid() 
			+ (((title==null)||title.isEmpty())?"":("_"+title)) 
			+ (withFileExtension?".txt":"");
	};

}

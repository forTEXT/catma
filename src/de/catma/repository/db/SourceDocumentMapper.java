package de.catma.repository.db;

import static de.catma.repository.db.jooq.catmarepository.Tables.SOURCEDOCUMENT;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;

import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;
import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentHandler;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;

public class SourceDocumentMapper implements
		RecordMapper<Record, SourceDocument> {
	
	private String sourceDocsPath;
	private FileURLFactory fileURLFactory;
	private Map<Integer, Result<Record>> userDefSepCharsRecords;
	private Map<Integer, Result<Record>> unseparableCharSeqRecords;
	private Map<Integer, Result<Record>> userMarkupCollectionRecords;

	private UserDefinedSeparatingCharacterMapper userDefCharMapper = 
			new UserDefinedSeparatingCharacterMapper();
	private UnseparableCharacterSequenceMapper unseparableCharacterSequenceMapper = 
			new UnseparableCharacterSequenceMapper();
	private UserMarkupCollectionReferenceMapper userMarkupCollectionReferenceMapper =
			new UserMarkupCollectionReferenceMapper();
	
	public SourceDocumentMapper(
			String sourceDocsPath, 
			Map<Integer, Result<Record>> userDefSepCharsRecords, 
			Map<Integer, Result<Record>> unseparableCharSeqRecords, 
			Map<Integer, Result<Record>> userMarkupCollectionRecords) {
		this.sourceDocsPath = sourceDocsPath;
		this.userDefSepCharsRecords = userDefSepCharsRecords;
		this.unseparableCharSeqRecords = unseparableCharSeqRecords;
		this.userMarkupCollectionRecords = userMarkupCollectionRecords;
		this.fileURLFactory = new FileURLFactory();
	}

	public SourceDocument map(Record record) {
		Integer sourceDocumentId = record.getValue(SOURCEDOCUMENT.SOURCEDOCUMENTID);
				
		IndexInfoSet indexInfoSet = 
				new IndexInfoSet(
					mapUnseparableCharSeqRecords(
						this.unseparableCharSeqRecords.get(sourceDocumentId)),
					mapUserDefinedSeparatingCharRecords(
						this.userDefSepCharsRecords.get(sourceDocumentId)),
					new Locale(record.getValue(SOURCEDOCUMENT.LOCALE)));
		ContentInfoSet contentInfoSet = 
				new ContentInfoSet(
						record.getValue(SOURCEDOCUMENT.AUTHOR), 
						record.getValue(SOURCEDOCUMENT.DESCRIPTION),
						record.getValue(SOURCEDOCUMENT.PUBLISHER),
						record.getValue(SOURCEDOCUMENT.TITLE));
		
		TechInfoSet techInfoSet = 
				new TechInfoSet(
					FileType.valueOf(record.getValue(SOURCEDOCUMENT.FILETYPE)),
					(record.getValue(SOURCEDOCUMENT.CHARSET)==null)?null
							:Charset.forName(record.getValue(SOURCEDOCUMENT.CHARSET)),
					FileOSType.valueOf(record.getValue(SOURCEDOCUMENT.FILEOSTYPE)),
					record.getValue(SOURCEDOCUMENT.CHECKSUM),
					record.getValue(SOURCEDOCUMENT.XSLTDOCUMENTLOCALURI));
		try {
			techInfoSet.setURI(
					new URI(
						fileURLFactory.getFileURL(record.getValue(SOURCEDOCUMENT.LOCALURI), 
						sourceDocsPath)));
		} catch (URISyntaxException e) {
			throw new DataAccessException("error mapping record to sourcedocument", e);
		} 
		
		SourceDocumentInfo sourceDocumentInfo = 
				new SourceDocumentInfo(indexInfoSet, contentInfoSet, techInfoSet);
		SourceDocumentHandler sdh = new SourceDocumentHandler();
		
		try {
			SourceDocument 	sourceDocument = sdh.loadSourceDocument(
				record.getValue(SOURCEDOCUMENT.LOCALURI), 
				sourceDocumentInfo);
			if (userMarkupCollectionRecords.containsKey(sourceDocumentId)) {
				for (Record r : 
					userMarkupCollectionRecords.get(sourceDocumentId)) {
					sourceDocument.addUserMarkupCollectionReference(
						userMarkupCollectionReferenceMapper.map(r));
				}
			}
			return sourceDocument;
		} catch (Exception e) {
			throw new DataAccessException("error loading sourcedocument", e);
		}
	
	}
	
	private List<String> mapUnseparableCharSeqRecords(Result<Record> unseparableCharSeqRecords) {
		List<String> result = new ArrayList<String>();
		if (unseparableCharSeqRecords != null) {
			for (Record r : unseparableCharSeqRecords) {
				result.add(unseparableCharacterSequenceMapper.map(r));
			}
		}
		return result;
	}

	private List<Character> mapUserDefinedSeparatingCharRecords(Result<Record> userDefSepCharRecords) {
		List<Character> result = new ArrayList<Character>();
		if (userDefSepCharRecords != null) {
			for (Record r : userDefSepCharRecords) {
				result.add(userDefCharMapper.map(r));
			}
		}
		return result;
	}
}

package de.catma.repository.db.mapper;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.CORPUS;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERMARKUPCOLLECTION;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.RecordMapper;
import org.jooq.Result;

import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

public class CorpusMapper implements RecordMapper<Record, Corpus> {

	private Repository dbRepository;
	private Map<Integer, Result<Record2<Integer, String>>> corpusSourceDocs;
	private Map<Integer, Result<Record2<Integer, Integer>>> corpusUmcs;

	public CorpusMapper(Repository dbRepository,
			Map<Integer, Result<Record2<Integer, String>>> corpusSourceDocs,
			Map<Integer, Result<Record2<Integer, Integer>>> corpusUmcs) {

		this.dbRepository = dbRepository;
		this.corpusSourceDocs = corpusSourceDocs;
		this.corpusUmcs = corpusUmcs;
	}

	public Corpus map(Record record) {
		Corpus corpus = new Corpus(
			String.valueOf(record.getValue(CORPUS.CORPUSID)),
			record.getValue(CORPUS.NAME));
		addSourceDocs(record.getValue(CORPUS.CORPUSID), corpus);
		return corpus;
	}

	private void addSourceDocs(Integer corpusId, Corpus corpus) {
		if (corpusSourceDocs.containsKey(corpusId)) {
			
			for (Record2<Integer, String> record : corpusSourceDocs.get(corpusId)) {
				SourceDocument sd = dbRepository.getSourceDocument(
						record.getValue(SOURCEDOCUMENT.LOCALURI));
					
				if (sd != null) {
					corpus.addSourceDocument(sd);

					if (corpusUmcs.containsKey(corpusId)) {
						Set<String> umcIdSet = new HashSet<String>();
						
						for (Record2<Integer, Integer> umcRecord : corpusUmcs.get(corpusId)) {
							umcIdSet.add(
								String.valueOf(
									umcRecord.getValue(
										USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID)));
						}
						
						for (UserMarkupCollectionReference umcRef : 
							sd.getUserMarkupCollectionRefs()) {
							
							if (umcIdSet.contains(umcRef.getId())) {
								corpus.addUserMarkupCollectionReference(umcRef);
							}
						}
					}
				}

			}
		}
		
	}
	
	
}

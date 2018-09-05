/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.indexer.db;

import static de.catma.repository.db.jooqgen.catmaindex.Tables.POSITION;
import static de.catma.repository.db.jooqgen.catmaindex.Tables.TERM;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.sql.DataSource;

import org.jooq.BatchBindStep;
import org.jooq.Record;
import org.jooq.SQLDialect;

import de.catma.document.source.SourceDocument;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;
import de.catma.repository.db.CatmaDataSourceName;
import de.catma.repository.db.jooq.TransactionalDSLContext;

class SourceDocumentIndexer {
	private DataSource dataSource;

	public SourceDocumentIndexer() {
		this.dataSource = CatmaDataSourceName.CATMADS.getDataSource();
	}

	void index(SourceDocument sourceDocument) throws IOException {
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
				
		try {
			List<String> unseparableCharacterSequences = 
					sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
						.getIndexInfoSet().getUnseparableCharacterSequences();
			List<Character> userDefinedSeparatingCharacters = 
					sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
						.getIndexInfoSet().getUserDefinedSeparatingCharacters();
			Locale locale = 
					sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
					.getIndexInfoSet().getLocale();
			
			TermExtractor termExtractor = 
					new TermExtractor(
						sourceDocument.getContent(), 
						unseparableCharacterSequences, 
						userDefinedSeparatingCharacters, 
						locale);
			
			Map<String, List<TermInfo>> terms = termExtractor.getTerms();
			
			db.beginTransaction();
			
			BatchBindStep termInsertBatch = db.batch(db
				.insertInto(
						TERM,
							TERM.DOCUMENTID,
							TERM.FREQUENCY,
							TERM.TERM_)
					.values(
						(String)null,
						null,
						null));
							
			for (Map.Entry<String, List<TermInfo>> entry : terms.entrySet()) {
				termInsertBatch.bind(
					sourceDocument.getID(),
					entry.getValue().size(),
					entry.getKey());
			}
			
			termInsertBatch.execute();

			Map<String, Record> termRecordsByTerm = db
			.select()
			.from(TERM)
			.where(TERM.DOCUMENTID.eq(sourceDocument.getID()))
			.fetchMap(TERM.TERM_);
			
			BatchBindStep posInsertBatch = db.batch(db
				.insertInto(
					POSITION,
						POSITION.TERMID,
						POSITION.CHARACTERSTART,
						POSITION.CHARACTEREND,
						POSITION.TOKENOFFSET)
				.values(
					(Integer)null,
					(Integer)null,
					(Integer)null,
					(Integer)null));
			
			for (Map.Entry<String, List<TermInfo>> entry : terms.entrySet()) {
				Record termRecord = termRecordsByTerm.get(entry.getKey());
				if (termRecord == null) {
					throw new IllegalStateException("no record for term " + entry.getKey());
				}
				Integer termId = termRecord.getValue(TERM.TERMID);
				
				for (TermInfo ti : entry.getValue()) {
					posInsertBatch.bind(
						termId, 
						ti.getRange().getStartPoint(), ti.getRange().getEndPoint(), 
						ti.getTokenOffset());
				}
			}
			
			posInsertBatch.execute();
			db.commitTransaction();
		}
		catch (Exception e) {
			db.rollbackTransaction();
			db.close();
			throw new IOException(e);
		}
		finally {
			if (db!=null) {
				db.close();
			}
		}
	}

	void removeSourceDocument(String sourceDocumentID) throws IOException {
		
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
				

		try {
			
			db.batch(
				db
				.delete(POSITION)
				.where(POSITION.TERMID.in(db
						.select(TERM.TERMID)
						.from(TERM)
						.where(TERM.DOCUMENTID.eq(sourceDocumentID)))),
				db
				.delete(TERM)
				.where(TERM.DOCUMENTID.eq(sourceDocumentID)))
			.execute();
			
			db.commitTransaction();
		}
		catch (Exception e) {
			db.rollbackTransaction();
			db.close();
			throw new IOException(e);
		}
		finally {
			if (db!=null) {
				db.close();
			}
		}
	}
}

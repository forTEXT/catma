package de.catma.indexer.db;

import static de.catma.repository.db.jooqgen.catmaindex.Tables.TERM;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.SOURCEDOCUMENT;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import de.catma.repository.db.CatmaDataSourceName;
import de.catma.repository.db.maintenance.SourceDocumentIndexMaintainer;

public class SourceDocumentDBIndexMaintainer implements
		SourceDocumentIndexMaintainer {
	
	private Logger logger = Logger.getLogger(SourceDocumentDBIndexMaintainer.class.getName());

	@Override
	public int checkSourceDocumentIndex(int maxObjectCount, int offset)
			throws Exception {

		DataSource dataSource = CatmaDataSourceName.CATMADS.getDataSource();
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);

		Result<Record2<Integer, String>> result = db
		.select(TERM.TERMID, TERM.DOCUMENTID)
		.from(TERM)
		.limit(offset, maxObjectCount)
		.fetch();
		
		offset += result.size();
				
		ArrayList<Integer> toBeDeleted = new ArrayList<Integer>();

		for (Record2<Integer,String> record : result) {
			String documentId = record.value2();
			Record1<Integer> repoRow = db
			.selectOne()
			.from(SOURCEDOCUMENT)
			.where(SOURCEDOCUMENT.LOCALURI.eq(documentId))
			.fetchOne();
			if (repoRow == null) {
				logger.info("index term row " + record + " is stale and will be removed");
				toBeDeleted.add(record.value1());
			}
		}

		logger.info("index term entries " + toBeDeleted + " are removed from the index");
		if (!toBeDeleted.isEmpty()) {
			db
			.delete(TERM)
			.where(TERM.TERMID.in(toBeDeleted))
			.execute();
			offset -= toBeDeleted.size();
		}

		return offset;
	}

}

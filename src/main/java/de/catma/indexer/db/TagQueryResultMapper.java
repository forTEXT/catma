package de.catma.indexer.db;

import static de.catma.repository.db.jooqgen.catmaindex.Tables.TAGREFERENCE;

import java.util.List;

import org.jooq.Record;
import org.jooq.RecordMapper;

import de.catma.document.Range;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.util.IDGenerator;

public class TagQueryResultMapper implements RecordMapper<Record, TagQueryResultRow> {

	private IDGenerator idGenerator;
	private List<Range> ranges;
	
	public TagQueryResultMapper(List<Range> ranges) {
		this.idGenerator = new IDGenerator();
		this.ranges = ranges;
	}
	
	public TagQueryResultRow map(Record record) {
		return new TagQueryResultRow(
				record.getValue(TAGREFERENCE.DOCUMENTID),
				ranges, 
				record.getValue(TAGREFERENCE.USERMARKUPCOLLECTIONID),
				idGenerator.uuidBytesToCatmaID(record.getValue(TAGREFERENCE.TAGDEFINITIONID)),
				record.getValue(TAGREFERENCE.TAGDEFINITIONPATH),
				record.getValue(TAGREFERENCE.TAGDEFINITIONVERSION),
				idGenerator.uuidBytesToCatmaID(record.getValue(TAGREFERENCE.TAGINSTANCEID)));
	}
}

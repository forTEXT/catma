package de.catma.repository.db;

import static de.catma.repository.db.jooq.catmarepository.Tables.TAGLIBRARY;

import org.jooq.Record;
import org.jooq.RecordMapper;

import de.catma.document.source.ContentInfoSet;
import de.catma.tag.TagLibraryReference;

public class TagLibraryReferenceMapper implements
		RecordMapper<Record, TagLibraryReference> {

	public TagLibraryReference map(Record record) {
		return new TagLibraryReference(
			String.valueOf(record.getValue(TAGLIBRARY.TAGLIBRARYID)),
			new ContentInfoSet(
				record.getValue(TAGLIBRARY.AUTHOR),
				record.getValue(TAGLIBRARY.DESCRIPTION),
				record.getValue(TAGLIBRARY.PUBLISHER),
				record.getValue(TAGLIBRARY.TITLE)));
	}
}

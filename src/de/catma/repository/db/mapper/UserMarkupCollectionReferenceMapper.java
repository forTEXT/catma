package de.catma.repository.db.mapper;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERMARKUPCOLLECTION;

import org.jooq.Record;
import org.jooq.RecordMapper;

import de.catma.document.source.ContentInfoSet;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

public class UserMarkupCollectionReferenceMapper implements
		RecordMapper<Record, UserMarkupCollectionReference> {

	public UserMarkupCollectionReference map(Record record) {
		return new UserMarkupCollectionReference(
			String.valueOf(record.getValue(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID)),
			null,
			new ContentInfoSet(
				record.getValue(USERMARKUPCOLLECTION.AUTHOR),
				record.getValue(USERMARKUPCOLLECTION.DESCRIPTION),
				record.getValue(USERMARKUPCOLLECTION.PUBLISHER),
				record.getValue(USERMARKUPCOLLECTION.TITLE)));
	}
}

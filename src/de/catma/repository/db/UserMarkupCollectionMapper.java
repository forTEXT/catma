package de.catma.repository.db;

import static de.catma.repository.db.jooq.catmarepository.Tables.USERMARKUPCOLLECTION;

import java.util.List;

import org.jooq.Record;
import org.jooq.RecordMapper;

import de.catma.document.source.ContentInfoSet;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.tag.TagLibrary;

public class UserMarkupCollectionMapper implements RecordMapper<Record, UserMarkupCollection> {

	private TagLibrary tagLibrary;
	private List<TagReference> tagReferences;
	
	public UserMarkupCollectionMapper(TagLibrary tagLibrary,
			List<TagReference> tagReferences) {
		this.tagLibrary = tagLibrary;
		this.tagReferences = tagReferences;
	}

	public UserMarkupCollection map(Record record) {
		return new UserMarkupCollection(
			String.valueOf(record.getValue(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID)),
			new ContentInfoSet(
				record.getValue(USERMARKUPCOLLECTION.AUTHOR),
				record.getValue(USERMARKUPCOLLECTION.DESCRIPTION),
				record.getValue(USERMARKUPCOLLECTION.PUBLISHER),
				record.getValue(USERMARKUPCOLLECTION.TITLE)),
			tagLibrary,
			tagReferences);
	}
}

package de.catma.repository.db.mapper;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERMARKUPCOLLECTION;

import java.util.List;

import de.catma.util.IDGenerator;
import org.jooq.Record;
import org.jooq.RecordMapper;

import de.catma.document.AccessMode;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.tag.TagLibrary;

public class UserMarkupCollectionMapper implements RecordMapper<Record, UserMarkupCollection> {

	private TagLibrary tagLibrary;
	private List<TagReference> tagReferences;
	private AccessMode accessMode;
	
	public UserMarkupCollectionMapper(TagLibrary tagLibrary,
			List<TagReference> tagReferences, AccessMode accessMode) {
		this.tagLibrary = tagLibrary;
		this.tagReferences = tagReferences;
		this.accessMode = accessMode;
	}

	public UserMarkupCollection map(Record record) {
		return new UserMarkupCollection(
			new IDGenerator().uuidBytesToCatmaID(record.getValue(USERMARKUPCOLLECTION.UUID)),
			new ContentInfoSet(
				record.getValue(USERMARKUPCOLLECTION.AUTHOR),
				record.getValue(USERMARKUPCOLLECTION.DESCRIPTION),
				record.getValue(USERMARKUPCOLLECTION.PUBLISHER),
				record.getValue(USERMARKUPCOLLECTION.TITLE)),
			tagLibrary,
			"","");
	}
}

package de.catma.indexer.db;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYDEF_POSSIBLEVALUE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGSETDEFINITION;

import org.jooq.Record;
import org.jooq.RecordMapper;

import de.catma.tag.TagDefinitionPathInfo;

public class TagDefinitionPathInfoMapper implements
		RecordMapper<Record, TagDefinitionPathInfo> {
			
	
	public TagDefinitionPathInfo map(Record record) {
				
		return new TagDefinitionPathInfo(
			record.getValue("ciTagRef.tagDefintionPath", String.class),
			record.getValue(TAGSETDEFINITION.NAME),
			record.getValue(PROPERTYDEF_POSSIBLEVALUE.VALUE));
	}
			

}

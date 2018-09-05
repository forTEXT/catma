package de.catma.repository.db.mapper;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGINSTANCE;

import java.util.Map;

import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.Result;

import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.util.IDGenerator;

public class TagInstanceMapper implements
		RecordMapper<Record, TagInstance> {
	
	private TagLibrary tagLibrary;
	private Map<Integer, Result<Record>> propertyRecordsByTagInstanceId;
	private IDGenerator idGenerator;
	private Map<Integer, Result<Record>> propertyValueRecordsByPropertyId;
	
	public TagInstanceMapper(
		TagLibrary tagLibrary, 
		Map<Integer, Result<Record>> propertyRecordsByTagInstanceId, 
		Map<Integer, Result<Record>> propertyValueRecordsByPropertyId) {
		
		this.tagLibrary = tagLibrary;
		this.propertyRecordsByTagInstanceId = propertyRecordsByTagInstanceId;
		this.propertyValueRecordsByPropertyId = propertyValueRecordsByPropertyId;
		this.idGenerator = new IDGenerator();
	}

	public TagInstance map(Record record) {
		TagInstance ti = new TagInstance(
			idGenerator.uuidBytesToCatmaID(record.getValue(TAGINSTANCE.UUID)),
			tagLibrary.getTagDefinition(
				idGenerator.uuidBytesToCatmaID(record.getValue(TAGDEFINITION.UUID))));
		addProperties(record.getValue(TAGINSTANCE.TAGINSTANCEID), ti);
		return ti;
	}

	private void addProperties(Integer tagInstanceId, TagInstance ti) {
		PropertyMapper propertyMapper = 
				new PropertyMapper(
						ti.getTagDefinition(), 
						propertyValueRecordsByPropertyId);
		if (propertyRecordsByTagInstanceId.containsKey(tagInstanceId)) {
			for (Record r : propertyRecordsByTagInstanceId.get(tagInstanceId)) {
				Property p = propertyMapper.map(r);
				if (PropertyDefinition.SystemPropertyName.hasPropertyName(p.getName())) {
					ti.addSystemProperty(p);
				}
				else {
					ti.addUserDefinedProperty(p);
				}
			}
		}
		
	}

}

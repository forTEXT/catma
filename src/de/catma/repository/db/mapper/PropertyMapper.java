package de.catma.repository.db.mapper;

import static de.catma.repository.db.jooqgen.catmaindex.Tables.PROPERTY;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYVALUE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.Result;

import de.catma.tag.Property;
import de.catma.tag.TagDefinition;
import de.catma.util.IDGenerator;

public class PropertyMapper implements RecordMapper<Record, Property> {
	
	private TagDefinition tagDefinition;
	private IDGenerator idGenerator;
	private Map<Integer, Result<Record>> propertyValueRecordsByPropertyId;
	
	public PropertyMapper(
			TagDefinition tagDefinition, 
			Map<Integer, Result<Record>> propertyValueRecordsByPropertyId) {
		this.tagDefinition = tagDefinition;
		this.idGenerator = new IDGenerator();
		this.propertyValueRecordsByPropertyId = propertyValueRecordsByPropertyId;
	}

	public Property map(Record record) {
		return new Property(
			tagDefinition.getPropertyDefinition(
				idGenerator.uuidBytesToCatmaID(
						record.getValue(PROPERTYDEFINITION.UUID))),
				createValueList(record.getValue(PROPERTY.PROPERTYID)));
	}

	private List<String> createValueList(Integer propertyId) {
		List<String> values = new ArrayList<String>();
		if (propertyValueRecordsByPropertyId.containsKey(propertyId)) {
			for (Record r : propertyValueRecordsByPropertyId.get(propertyId)) {
				values.add(r.getValue(PROPERTYVALUE.VALUE));
			}
		}
		
		return values;
	}
}

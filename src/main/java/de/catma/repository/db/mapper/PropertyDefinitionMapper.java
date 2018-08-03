package de.catma.repository.db.mapper;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYDEF_POSSIBLEVALUE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jooq.Record;
import org.jooq.RecordMapper;

import de.catma.tag.PropertyDefinition;
import de.catma.util.IDGenerator;

public class PropertyDefinitionMapper implements
		RecordMapper<Record, PropertyDefinition> {

	private Map<String, List<Record>> possValuesByPropDefUuid;
	private IDGenerator idGenerator;

	public PropertyDefinitionMapper(
			Map<String, List<Record>> possValuesByPropDefUuid) {
		this.possValuesByPropDefUuid = possValuesByPropDefUuid;
		this.idGenerator = new IDGenerator();
	}

	public PropertyDefinition map(Record record) {
		String propDefUuid = 
			idGenerator.uuidBytesToCatmaID(record.getValue(PROPERTYDEFINITION.UUID));
		return new PropertyDefinition(
			record.getValue(PROPERTYDEFINITION.NAME),
			getPossibleValuesList(possValuesByPropDefUuid.get(propDefUuid)));
	}

	private List<String> getPossibleValuesList(
			List<Record> possValuesRecords) {
		ArrayList<String> values = new ArrayList<String>();
		
		if (possValuesRecords != null) {
			for (Record r : possValuesRecords) {
				values.add(r.getValue(PROPERTYDEF_POSSIBLEVALUE.VALUE));
			}
		}
		
		return values;
	}
}

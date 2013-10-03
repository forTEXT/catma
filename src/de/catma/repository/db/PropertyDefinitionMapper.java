package de.catma.repository.db;

import static de.catma.repository.db.jooq.catmarepository.Tables.PROPERTYDEFINITION;
import static de.catma.repository.db.jooq.catmarepository.Tables.PROPERTYDEF_POSSIBLEVALUE;

import java.util.ArrayList;
import java.util.Map;

import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.Result;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyPossibleValueList;
import de.catma.util.IDGenerator;

public class PropertyDefinitionMapper implements
		RecordMapper<Record, PropertyDefinition> {

	private Map<byte[], Result<Record>> possValuesByDefUuid;
	private IDGenerator idGenerator;

	public PropertyDefinitionMapper(
			Map<byte[], Result<Record>> possValuesByDefUuid) {
		this.possValuesByDefUuid = possValuesByDefUuid;
		this.idGenerator = new IDGenerator();
	}

	public PropertyDefinition map(Record record) {

		return new PropertyDefinition(
			record.getValue(PROPERTYDEFINITION.PROPERTYDEFINITIONID),
			idGenerator.uuidBytesToCatmaID(record.getValue(PROPERTYDEFINITION.UUID)),
			record.getValue(PROPERTYDEFINITION.NAME),
			getPossibleValuesList(possValuesByDefUuid.get(record.getValue(PROPERTYDEFINITION.UUID))));
	}

	private PropertyPossibleValueList getPossibleValuesList(
			Result<Record> possValuesRecords) {
		ArrayList<String> values = new ArrayList<String>();
		
		if (possValuesRecords != null) {
			for (Record r : possValuesRecords) {
				values.add(r.getValue(PROPERTYDEF_POSSIBLEVALUE.VALUE));
			}
		}
		
		return new PropertyPossibleValueList(new ArrayList<String>(), true);
	}
}

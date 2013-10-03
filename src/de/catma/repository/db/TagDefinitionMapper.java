package de.catma.repository.db;

import static de.catma.repository.db.jooq.catmarepository.Tables.TAGDEFINITION;

import java.util.Map;

import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.Result;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.Version;
import de.catma.util.IDGenerator;

public class TagDefinitionMapper implements RecordMapper<Record, TagDefinition> {

	private Map<byte[], Result<Record>> propertyDefByTagDefUuid;
	private IDGenerator idGenerator;
	private PropertyDefinitionMapper propertyDefinitionMapper;
		
	public TagDefinitionMapper(
			Map<byte[], Result<Record>> propertyDefByTagDefUuid,
			Map<byte[], Result<Record>> possValuesByDefUuid) {
		this.propertyDefByTagDefUuid = propertyDefByTagDefUuid;
		this.idGenerator = new IDGenerator();
		this.propertyDefinitionMapper = new PropertyDefinitionMapper(possValuesByDefUuid);
	}

	public TagDefinition map(Record record) {
		TagDefinition tagDefinition = 
				new TagDefinition(
					record.getValue(TAGDEFINITION.TAGDEFINITIONID),
					idGenerator.uuidBytesToCatmaID(record.getValue(TAGDEFINITION.UUID)),
					record.getValue(TAGDEFINITION.NAME),
					new Version(record.getValue(TAGDEFINITION.VERSION)),
					record.getValue(TAGDEFINITION.PARENTID),
					idGenerator.uuidBytesToCatmaID(record.getValue(TAGDEFINITION.PARENTUUID)));
					
		addPropertyDefinitions(tagDefinition, record.getValue(TAGDEFINITION.UUID));				
			
		return tagDefinition;
	}

	private void addPropertyDefinitions(TagDefinition tagDefinition,
			byte[] uuid) {
		if (propertyDefByTagDefUuid.containsKey(uuid)) {
			for (Record r : propertyDefByTagDefUuid.get(uuid)) {
				PropertyDefinition pd = propertyDefinitionMapper.map(r);
				if (PropertyDefinition.SystemPropertyName.hasPropertyName(pd.getName())) {
					tagDefinition.addSystemPropertyDefinition(pd);
				}
				else {
					tagDefinition.addUserDefinedPropertyDefinition(pd);
				}
			}
		}
		
	}
}

package de.catma.repository.db.mapper;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGDEFINITION;

import java.util.List;
import java.util.Map;

import org.jooq.Record;
import org.jooq.RecordMapper;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.Version;
import de.catma.util.IDGenerator;

public class TagDefinitionMapper implements RecordMapper<Record, TagDefinition> {

	private Map<String, List<Record>> propertyDefByTagDefUuid;
	private IDGenerator idGenerator;
	private PropertyDefinitionMapper propertyDefinitionMapper;
		
	public TagDefinitionMapper(
			Map<String, List<Record>> propertyDefByTagDefUuid,
			Map<String, List<Record>> possValuesByPropDefUuid) {
		this.propertyDefByTagDefUuid = propertyDefByTagDefUuid;
		this.idGenerator = new IDGenerator();
		this.propertyDefinitionMapper = new PropertyDefinitionMapper(possValuesByPropDefUuid);
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
					
		addPropertyDefinitions(tagDefinition);				
			
		return tagDefinition;
	}

	private void addPropertyDefinitions(TagDefinition tagDefinition) {
		
		if (propertyDefByTagDefUuid.containsKey(tagDefinition.getUuid())) {
			for (Record r : propertyDefByTagDefUuid.get(tagDefinition.getUuid())) {
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

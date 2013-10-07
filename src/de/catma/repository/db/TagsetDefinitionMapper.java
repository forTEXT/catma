package de.catma.repository.db;

import static de.catma.repository.db.jooq.catmarepository.Tables.TAGSETDEFINITION;

import java.util.List;
import java.util.Map;

import org.jooq.Record;
import org.jooq.RecordMapper;

import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.util.IDGenerator;

public class TagsetDefinitionMapper implements
		RecordMapper<Record, TagsetDefinition> {
	
	private Map<String, List<Record>> tagDefByTagsetDefUuid;
	private IDGenerator idGenerator;
	private TagDefinitionMapper tagDefinitionMapper;
	
	public TagsetDefinitionMapper(
			Map<String, List<Record>> tagDefByTagsetDefUuid2,
			Map<String, List<Record>> propertyDefByTagDefUuid,
			Map<String, List<Record>> possValuesByDefUuid) {
		this.tagDefByTagsetDefUuid = tagDefByTagsetDefUuid2;
		this.idGenerator = new IDGenerator();
		this.tagDefinitionMapper = 
			new TagDefinitionMapper(propertyDefByTagDefUuid, possValuesByDefUuid);
	}

	public TagsetDefinition map(Record record) {
		TagsetDefinition tagsetDefinition = 
				new TagsetDefinition(
					record.getValue(TAGSETDEFINITION.TAGSETDEFINITIONID),
					idGenerator.uuidBytesToCatmaID(
						record.getValue(TAGSETDEFINITION.UUID)), 
					record.getValue(TAGSETDEFINITION.NAME),
					new Version(record.getValue(TAGSETDEFINITION.VERSION)));
		
		addTagDefinitions(tagsetDefinition);
		
		return tagsetDefinition;
	}




	private void addTagDefinitions(TagsetDefinition tagsetDefinition) {
		if (tagDefByTagsetDefUuid.containsKey(tagsetDefinition.getUuid())) {
			
			for (Record r : tagDefByTagsetDefUuid.get(tagsetDefinition.getUuid())) {
				tagsetDefinition.addTagDefinition(tagDefinitionMapper.map(r));
			}
		}
	}

}

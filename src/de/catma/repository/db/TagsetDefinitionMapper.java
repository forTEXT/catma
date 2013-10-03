package de.catma.repository.db;

import static de.catma.repository.db.jooq.catmarepository.Tables.TAGSETDEFINITION;

import java.util.Map;

import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.Result;

import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.util.IDGenerator;

public class TagsetDefinitionMapper implements
		RecordMapper<Record, TagsetDefinition> {
	
	private Map<byte[], Result<Record>> tagDefByTagsetDefUuid;
	private IDGenerator idGenerator;
	private TagDefinitionMapper tagDefinitionMapper;
	
	public TagsetDefinitionMapper(
			Map<byte[], Result<Record>> tagDefByTagsetDefUuid,
			Map<byte[], Result<Record>> propertyDefByTagDefUuid,
			Map<byte[], Result<Record>> possValuesByDefUuid) {
		this.tagDefByTagsetDefUuid = tagDefByTagsetDefUuid;
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
		
		addTagDefinitions(tagsetDefinition, record.getValue(TAGSETDEFINITION.UUID));
		
		return tagsetDefinition;
	}




	private void addTagDefinitions(TagsetDefinition tagsetDefinition, byte[] uuid) {
		if (tagDefByTagsetDefUuid.containsKey(uuid)) {
			
			for (Record r : tagDefByTagsetDefUuid.get(uuid)) {
				tagsetDefinition.addTagDefinition(tagDefinitionMapper.map(r));
			}
		}
	}

}

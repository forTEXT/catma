package de.catma.repository.db.mapper;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGINSTANCE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGREFERENCE;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.exception.DataAccessException;

import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.tag.TagInstance;
import de.catma.util.IDGenerator;

public class TagReferenceMapper implements
		RecordMapper<Record, TagReference> {

	private Map<String, TagInstance> tagInstancesByUUID = new HashMap<String, TagInstance>();
	private IDGenerator idGenerator;
	private String localSourceDocURI;
	
	public TagReferenceMapper(String localSourceDocURI, List<TagInstance> tagInstances) {
		this.localSourceDocURI = localSourceDocURI;
		for (TagInstance ti : tagInstances) {
			tagInstancesByUUID.put(ti.getUuid(), ti);
		}
		this.idGenerator = new IDGenerator();
	}
	
	public TagReference map(Record record) {
		try {
			return new TagReference(
				tagInstancesByUUID.get(idGenerator.uuidBytesToCatmaID(record.getValue(TAGINSTANCE.UUID))),
				localSourceDocURI,
				new Range(
						record.getValue(TAGREFERENCE.CHARACTERSTART),
						record.getValue(TAGREFERENCE.CHARACTEREND)));
		}
		catch(URISyntaxException e) {
			throw new DataAccessException("error loading TagReference", e);
		}
	}
}

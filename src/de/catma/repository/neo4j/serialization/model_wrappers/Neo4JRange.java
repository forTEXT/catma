package de.catma.repository.neo4j.serialization.model_wrappers;

import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity(label="Range")
public class Neo4JRange {
	@Id
	@GeneratedValue
	private Long id;

	private int startPoint;
	private int endPoint;

	public Neo4JRange() {

	}

	public Neo4JRange(Range range) {
		this();

		this.setRange(range);
	}

	public Neo4JRange(TagReference tagReference) {
		this();

		this.setRange(tagReference);
	}

	public Range getRange() {
		return new Range(this.startPoint, this.endPoint);
	}

	public void setRange(Range range) {
		this.startPoint = range.getStartPoint();
		this.endPoint = range.getEndPoint();
	}

	public void setRange(TagReference tagReference) {
		Range range = tagReference.getRange();
		this.setRange(range);
	}
}

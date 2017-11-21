package de.catma.repository.neo4j.models;

import de.catma.indexer.TermInfo;
import de.catma.repository.neo4j.model_wrappers.Neo4JRange;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity(label="Position")
public class Neo4JPosition extends Neo4JRange {
	private int offset;

	@Relationship(type="APPEARS_AFTER", direction=Relationship.OUTGOING)
	private Neo4JPosition positionBefore;

	@Relationship(type="APPEARS_BEFORE", direction=Relationship.OUTGOING)
	private Neo4JPosition positionAfter;

	public Neo4JPosition() {
		super();
	}

	public Neo4JPosition(TermInfo termInfo) {
		super(termInfo.getRange());

		this.setTermInfo(termInfo);
	}

	public int getOffset() {
		return this.offset;
	}

	public TermInfo getTermInfo() {
		return new TermInfo(null, this.startPoint, this.endPoint, this.offset);
	}

	public void setTermInfo(TermInfo termInfo) {
		this.positionBefore = null;
		this.positionAfter = null;

		this.offset = termInfo.getTokenOffset();
	}

	public Neo4JPosition getPositionBefore() {
		return this.positionBefore;
	}

	public void setPositionBefore(Neo4JPosition position) {
		this.positionBefore = position;
	}

	public Neo4JPosition getPositionAfter() {
		return this.positionAfter;
	}

	public void setPositionAfter(Neo4JPosition position) {
		this.positionAfter = position;
	}
}

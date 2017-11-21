package de.catma.repository.neo4j.models;

import de.catma.indexer.TermInfo;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@NodeEntity(label="Term")
public class Neo4JTerm {
	@Id
	@GeneratedValue
	private Long id;

	private String literal;
	private int frequency;

	@Relationship(type="HAS_POSITION", direction=Relationship.OUTGOING)
	private List<Neo4JPosition> positions;

	public Neo4JTerm() {
		this.positions = new ArrayList<>();
	}

	public Neo4JTerm(String term, List<TermInfo> termInfos) {
		this();

		this.literal = term;
		this.frequency = termInfos.size();

		this.setTermInfos(termInfos);
	}

	public Long getId() {
		return this.id;
	}

	public String getLiteral() {
		return this.literal;
	}

	public int getFrequency() {
		return this.frequency;
	}

	public List<TermInfo> getTermInfos() {
		return this.positions.stream().map(position -> {
			// the TermInfo object that Neo4JPosition gives us is not aware of the term literal, so we add it here
			TermInfo termInfo = position.getTermInfo();
			return new TermInfo(
					this.literal,
					termInfo.getRange().getStartPoint(),
					termInfo.getRange().getEndPoint(),
					termInfo.getTokenOffset()
			);
		}).collect(Collectors.toList());
	}

	public void setTermInfos(List<TermInfo> termInfos) {
		this.positions.clear();

		termInfos.sort(TermInfo.TOKENOFFSETCOMPARATOR);

		this.positions = termInfos.stream().map(Neo4JPosition::new).collect(Collectors.toList());

		for (int i=0; i<this.positions.size(); i++) {

			if (i > 0) {
				this.positions.get(i).setPositionBefore(this.positions.get(i-1));
			}

			if (i < this.positions.size() - 1) {
				this.positions.get(i).setPositionAfter(this.positions.get(i+1));
			}
		}
	}
}

package de.catma.repository.git.graph.lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import de.catma.document.Range;

class Term {
	
	private final String literal;
	private final int frequency;
	private List<Position> positions;
	
	public Term(String literal, int frequency) {
		super();
		this.literal = literal;
		this.frequency = frequency;
		this.positions = new ArrayList<Position>();
	}
	public String getLiteral() {
		return literal;
	}
	public int getFrequency() {
		return frequency;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((literal == null) ? 0 : literal.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Term))
			return false;
		Term other = (Term) obj;
		if (literal == null) {
			if (other.literal != null)
				return false;
		} else if (!literal.equals(other.literal))
			return false;
		return true;
	}
	
	public void addPosition(Position position) {
		positions.add(position);
	}

	public List<Position> getPositions() {
		return positions;
	}
	
	public List<List<Position>> getPositions(List<String> termLiteralList, BiPredicate<String, String> termTestFunction) {
		List<List<Position>> result = new ArrayList<List<Position>>();
		for (Position position : positions) {
			List<Position> positions = position.getPositionChain(termLiteralList, termTestFunction);
			if (positions != null) {
				result.add(positions);
			}
		}
		return result;
	}
	
	public Collection<? extends Position> getPositions(Range range) {
		return positions.stream().filter(pos -> range.hasOverlappingRange(pos.getRange())).collect(Collectors.toSet());
	}	
}
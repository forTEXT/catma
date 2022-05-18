package de.catma.repository.git.graph.lazy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import de.catma.document.Range;

class Position {
	private final int startOffset;
	private final int endOffset;
	private final int tokenOffset;
	private final Term term;
	private Position forwardAdjacentPostion;
	private Position backwardAdjacentPosition;
	
	public Position(int startOffset, int endOffset, int tokenOffset, Term term) {
		super();
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.tokenOffset = tokenOffset;
		this.term = term;
	}
	public int getStartOffset() {
		return startOffset;
	}
	public int getEndOffset() {
		return endOffset;
	}
	public int getTokenOffset() {
		return tokenOffset;
	}
	
	public Range getRange() {
		return new Range(startOffset, endOffset);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endOffset;
		result = prime * result + startOffset;
		result = prime * result + tokenOffset;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Position))
			return false;
		Position other = (Position) obj;
		if (endOffset != other.endOffset)
			return false;
		if (startOffset != other.startOffset)
			return false;
		if (tokenOffset != other.tokenOffset)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "Position [startOffset=" + startOffset + ", endOffset=" + endOffset + ", tokenOffset=" + tokenOffset
				+ "]";
	}
	
	public Term getTerm() {
		return term;
	}
	
	public void setForwardAdjacentPostion(Position adjacentPostion) {
		this.forwardAdjacentPostion = adjacentPostion;
	}
	
	public Position getForwardAdjacentPostion() {
		return forwardAdjacentPostion;
	}
	
	public void setBackwardAdjacentPosition(Position backwardAdjacentPosition) {
		this.backwardAdjacentPosition = backwardAdjacentPosition;
	}
	
	public Position getBackwardAdjacentPosition() {
		return backwardAdjacentPosition;
	}
	
	public List<Position> getPositionChain(List<String> termLiteralList, BiPredicate<String, String> termTestFunction) {
		List<Position> positions = new ArrayList<Position>();
		positions.add(this);
		Position curPos = this;
		for (String termLiteral : termLiteralList) {
			curPos = curPos.getForwardAdjacentPostion();
			if ((curPos != null) && termTestFunction.test(curPos.getTerm().getLiteral(), termLiteral)) {
				positions.add(curPos);
			}
			else {
				return null;
			}
		}
		
		
		return positions;
	}
}

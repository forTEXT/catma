package de.catma.repository.git.graph.tp;


import java.util.function.Predicate;

import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.catma.document.Range;

public class InRangeFilter implements Predicate<Traverser<Vertex>> {
	
	private Range rangeToTest;
	
	public InRangeFilter(Range rangeToTest) {
		super();
		this.rangeToTest = rangeToTest;
	}

	@Override
	public boolean test(Traverser<Vertex> t) {
		
		int startOffset = (int) t.get().property("startOffset").value();
		int endOffset = (int) t.get().property("endOffset").value();

		Range positionRange = new Range(startOffset, endOffset);
		
		return rangeToTest.hasOverlappingRange(positionRange);
	}

}

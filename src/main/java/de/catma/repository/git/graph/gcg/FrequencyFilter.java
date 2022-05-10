package de.catma.repository.git.graph.gcg;

import java.util.function.Predicate;

import de.catma.queryengine.CompareOperator;

public class FrequencyFilter implements Predicate<Term> {

	private CompareOperator comp1;
	private int freq1;
	private CompareOperator comp2;
	private int freq2;

	public FrequencyFilter(CompareOperator comp1, int freq1, CompareOperator comp2, int freq2) {
		this.comp1 = comp1;
		this.freq1 = freq1;
		this.comp2 = comp2;
		this.freq2 = freq2;
	}

	@Override
	public boolean test(Term t) {
		int freq = t.getFrequency();
		
		if (comp1.getCondition().isTrue(freq, freq1)) {
			if (comp2 == null) {
				return true;
			}
			else if (comp2.getCondition().isTrue(freq, freq2)) {
				return true;
			}
		}		
		
		return false;
	}

}

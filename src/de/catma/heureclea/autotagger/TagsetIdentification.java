package de.catma.heureclea.autotagger;

public enum TagsetIdentification {
	pos("Parts Of Speech"),
	tense("Tense"),
	temporal_signals("Temporal Signals (simple)"),
	temporal_signals_hybrid("Temporal Signals (hybrid)"),
	;
	
	private String description;

	private TagsetIdentification(String description) {
		this.description = description;
	}
	
	@Override
	public String toString() {
		return description;
	}
}

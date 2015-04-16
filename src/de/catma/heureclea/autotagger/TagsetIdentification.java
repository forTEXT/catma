package de.catma.heureclea.autotagger;

public enum TagsetIdentification {
	pos("Parts Of Speech German"),
	tense("Tense German"),
	temporal_signals_hybrid("Temporal Signals German"),
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

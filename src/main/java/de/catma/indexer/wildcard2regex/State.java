package de.catma.indexer.wildcard2regex;

public enum State {
	DEFAULT(new DefaultStateHandler()),
	ESCAPE(new EscapeStateHandler()),
	
	;
	private StateHandler handler;

	private State(StateHandler handler) {
		this.handler = handler;
	}

	public StateHandler getHandler() {
		return handler;
	}
}

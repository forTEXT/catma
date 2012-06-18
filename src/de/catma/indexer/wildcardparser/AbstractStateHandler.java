package de.catma.indexer.wildcardparser;

import java.util.List;

import de.catma.indexer.TermInfo;

public abstract class AbstractStateHandler implements StateHandler {
	protected List<TermInfo> orderedTermInfos;

	public AbstractStateHandler(List<TermInfo> orderedTermInfos) {
		this.orderedTermInfos = orderedTermInfos;
	}
}
package de.catma.ui.analyzer;

import de.catma.document.Corpus;
import de.catma.indexer.IndexedRepository;

public interface AnalyzerProvider {

	public void analyze(
			Corpus corpus,
			IndexedRepository repository);

}

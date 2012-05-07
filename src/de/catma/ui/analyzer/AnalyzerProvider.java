package de.catma.ui.analyzer;

import de.catma.core.document.Corpus;
import de.catma.core.document.repository.Repository;

public interface AnalyzerProvider {

	public void analyze(
			Corpus corpus,
			Repository repository);

}

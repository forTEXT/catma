package de.catma.heureclea.autotagger;

import de.catma.backgroundservice.DefaultProgressCallable;

public class AnnotationGeneratorJob extends DefaultProgressCallable<Void> {
	
	private GenerationOptions generationOptions;

	public AnnotationGeneratorJob(GenerationOptions generationOptions) {
		super();
		this.generationOptions = generationOptions;
	}

	@Override
	public Void call() throws Exception {
		new AnnotationGenerator().generate(
			generationOptions.getCorpusId(), 
			generationOptions.getTagsetIdentification());
		
		return null;
	}
}

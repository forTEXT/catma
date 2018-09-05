package de.catma.ui.repository;

import de.catma.ui.field.GeneratorFieldFactory;

public class GenerationOptionsFieldFactory extends GeneratorFieldFactory {

	public GenerationOptionsFieldFactory() {
		this.formFieldGenerators.put(
				"tagsetIdentification", new TagsetIdentificationFieldGenerator()); //$NON-NLS-1$
	}

}

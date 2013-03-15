package de.catma.ui.repository.sharing;

import de.catma.ui.field.GeneratorFieldFactory;

public class SharingOptionsFieldFactory extends GeneratorFieldFactory {

	public SharingOptionsFieldFactory() {
		this.formFieldGenerators.put("accessMode", new AccessModeFieldGenerator());
		this.formFieldGenerators.put("userIdentification", new UserIdentFieldGenerator());
	}
}

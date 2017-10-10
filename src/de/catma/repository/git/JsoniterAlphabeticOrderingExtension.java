package de.catma.repository.git;

import com.jsoniter.spi.*;

import java.util.Comparator;

public class JsoniterAlphabeticOrderingExtension {
	private static boolean enabled;

	public static synchronized void enable() {
		if (enabled) {
			throw new JsonException("JsoniterAlphabeticOrderingExtension.enable can only be called once");
		}
		enabled = true;
		JsoniterSpi.registerExtension(new EmptyExtension() {
			@Override
			public void updateClassDescriptor(ClassDescriptor desc) {
				if (desc.getters != null) {
					desc.getters.sort(Comparator.comparing(b -> b.name));
				}
			}
		});
	}
}

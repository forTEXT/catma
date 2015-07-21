package de.catma.i18n;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class HelpTexts {
	private static final String BUNDLE_NAME = "de.catma.i18n.help-texts"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);

	private HelpTexts() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}

package de.catma;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import de.catma.properties.CATMAProperties;

public class PropertiesHelper {
	public static void load() throws IOException {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";
				Properties catmaProperties = new Properties();
		try {
			catmaProperties.load(new FileInputStream(propertiesFile));
			CATMAProperties.INSTANCE.setProperties(catmaProperties);
		}
		catch (FileNotFoundException fnf) {
			throw new FileNotFoundException(
				"Couldn't find properties file: " + new File(propertiesFile).getAbsolutePath());
		}
	}
}

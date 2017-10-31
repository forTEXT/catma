package de.catma.repository.neo;

import de.catma.repository.neo.exceptions.NeoTagsetHandlerException;
import de.catma.repository.neo.managers.Neo4JGraphManager;
import de.catma.tag.TagsetDefinition;
import de.catma.util.IDGenerator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.FileInputStream;
import java.util.Properties;

import static org.junit.Assert.*;

public class NeoTagsetHandlerTest {
	private Properties catmaProperties;

	private IDGenerator idGenerator;

	public NeoTagsetHandlerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));

		this.idGenerator = new IDGenerator();
	}
	// how to test for exceptions: https://stackoverflow.com/a/31826781
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void insertTagset() throws Exception {
		try (Neo4JGraphManager graphManager = new Neo4JGraphManager(this.catmaProperties)) {

			NeoTagsetHandler neoTagsetHandler = new NeoTagsetHandler(graphManager);

			String uuid = this.idGenerator.generate();

			TagsetDefinition tagsetDefinition = new TagsetDefinition();
			tagsetDefinition.setUuid(uuid);
			tagsetDefinition.setName("ALovelyName");

			neoTagsetHandler.insertTagset(tagsetDefinition);
		}
	}

}

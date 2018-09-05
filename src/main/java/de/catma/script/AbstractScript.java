package de.catma.script;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

public class AbstractScript {
	protected Logger logger = Logger.getLogger(this.getClass().getName());

	protected Representation tryGet(ClientResource client) {
		int i=10;
		while (i>0) {
			try {
				Representation rep = client.get();
				return rep;
			}
			catch (Exception e) {
				logger.warning("tryGet failed " + i + " " + e.getMessage());
			}
			i--;
		}		
		
		throw new RuntimeException("tryGet failed too many times");
	}

	protected void loadFromFile(String fileName, Set<String> container) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (FileInputStream fis = new FileInputStream(fileName)) {
			IOUtils.copy(fis, buffer);
		}
		
		String[] names = buffer.toString("UTF-8").split(",");
		for (String name : names) {
			if (!name.trim().isEmpty()) {
				container.add(name.trim());
			}
		}
		
	}

}

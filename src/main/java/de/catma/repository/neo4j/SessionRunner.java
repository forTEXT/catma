package de.catma.repository.neo4j;

import org.neo4j.driver.v1.Session;

public interface SessionRunner {
	public void run(Session session) throws Exception;
}

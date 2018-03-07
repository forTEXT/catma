package de.catma.repository.neo4j;

import org.neo4j.driver.v1.Session;

import de.catma.indexer.graph.CatmaGraphDbName;

public class StatementExcutor {
	public static void execute(SessionRunner sessionRunner) throws Exception {
		try (Session session = CatmaGraphDbName.CATMAGRAPHDB.getBoltSession()) {
			sessionRunner.run(session);
		}
	}

}

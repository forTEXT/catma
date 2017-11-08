package de.catma.repository.neo4j.managers;

import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import java.util.Properties;

public class Neo4JOGMSessionFactory implements AutoCloseable {
	private SessionFactory sessionFactory;

	public Neo4JOGMSessionFactory(String uri, String userName, String password){
		Configuration configuration = new Configuration.Builder()
				.uri(uri)
				.credentials(userName, password)
				.build();

		this.sessionFactory = new SessionFactory(configuration,"de.catma.repository.neo4j");
		sessionFactory.close();
	}

	public Neo4JOGMSessionFactory(Properties catmaProperties){
		this(
				catmaProperties.getProperty("Neo4JBoltUrl"),
				catmaProperties.getProperty("Neo4JAdminUser"),
				catmaProperties.getProperty("Neo4JAdminPassword")
		);
	}

	public Session getSession(){
		return this.sessionFactory.openSession();
	}

	@Override
	public void close() throws Exception {
		SessionFactory safeFactory = this.sessionFactory;
		if (safeFactory != null) {
			safeFactory.close();
			this.sessionFactory = null;
		}
	}
}

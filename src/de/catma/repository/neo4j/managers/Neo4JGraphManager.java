package de.catma.repository.neo4j.managers;

import java.util.ArrayList;
import java.util.Properties;

import org.neo4j.driver.v1.*;

public class Neo4JGraphManager implements AutoCloseable {
	private Driver driver;
	private ArrayList<Session> sessions = new ArrayList<>();

	public Neo4JGraphManager(Properties catmaProperties) {
		String boltUrl = catmaProperties.getProperty("Neo4JBoltUrl");
		String adminUserName = catmaProperties.getProperty("Neo4JAdminUser");
		String password = catmaProperties.getProperty("Neo4JAdminPassword");

		this.driver = GraphDatabase.driver( boltUrl, AuthTokens.basic( adminUserName, password ) );
	}

	public Session openSession(){
		Session session = this.driver.session();
		this.sessions.add(session);
		return session;
	}

	@Override
	public void close() throws Exception {
		for(Session session : sessions){
			if(session.isOpen()){
				session.close();
			}
		}

		Driver safeDriver = this.driver;
		if (safeDriver != null) {
			safeDriver.close();
			this.driver = null;
		}
	}
}

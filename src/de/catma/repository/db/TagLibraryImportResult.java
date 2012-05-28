package de.catma.repository.db;

import java.util.Map;

import org.hibernate.Session;

import de.catma.repository.db.model.DBTagDefinition;
import de.catma.repository.db.model.DBTagLibrary;

public class TagLibraryImportResult {

	private Session session;
	private DBTagLibrary dbTagLibrary;
	private Map<String, DBTagDefinition> dbTagDefinitons;
	
	public TagLibraryImportResult(Session session, DBTagLibrary dbTagLibrary,
			Map<String, DBTagDefinition> dbTagDefinitons) {
		this.session = session;
		this.dbTagLibrary = dbTagLibrary;
		this.dbTagDefinitons = dbTagDefinitons;
	}

	public Session getSession() {
		return session;
	}

	public DBTagLibrary getDbTagLibrary() {
		return dbTagLibrary;
	}

	public Map<String, DBTagDefinition> getDbTagDefinitons() {
		return dbTagDefinitons;
	}
	
	
	
}

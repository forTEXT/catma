package de.catma.repository.db;

import org.hibernate.Session;

import de.catma.repository.db.model.DBTagLibrary;

public class TagLibraryImportResult {

	private Session session;
	private DBTagLibrary dbTagLibrary;
	
	public TagLibraryImportResult(Session session, DBTagLibrary dbTagLibrary) {
		this.session = session;
		this.dbTagLibrary = dbTagLibrary;
	}

	public Session getSession() {
		return session;
	}

	public DBTagLibrary getDbTagLibrary() {
		return dbTagLibrary;
	}

}

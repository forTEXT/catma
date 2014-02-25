package de.catma.repository.db.maintenance;

import java.io.IOException;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import de.catma.repository.db.CatmaDataSourceName;

public class DBIndexMaintainer {

	private Logger logger;

	public DBIndexMaintainer() {
		this.logger = Logger.getLogger(DBIndexMaintainer.class.getName());
	}
	
	public void run() throws IOException {
		try {
			Context  context = new InitialContext();
			DataSource dataSource = (DataSource) context.lookup(
					CatmaDataSourceName.CATMADS.name());
			DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);

			// all repo.tagreferences need an entry in index.tagref
			// all repo.propertyvalues need an entry in index.property
			
			// delete all index.tagref that are no longer in repo.tagreference
			// delete all index.proeprty that are no longer in repo.propertyvalue
			
			checkTagReferences(db);
		}
		catch (Exception e) {
			throw new IOException(e);
		}	
	}

	private void checkTagReferences(DSLContext db) {
		
	}
}

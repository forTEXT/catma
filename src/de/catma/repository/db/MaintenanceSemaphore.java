package de.catma.repository.db;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.MAINTENANCE_SEM;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import de.catma.repository.db.jooq.TransactionalDSLContext;
import de.catma.repository.db.mapper.IDFieldToIntegerMapper;

public class MaintenanceSemaphore {
	
	public class AccessDeniedException extends IllegalStateException {
		
	}
	
	public enum Type {
		CLEANING,
		IMPORT,
		SYNCH,
		;
		

		public String[] getExclusionTypes() {
			switch (this) {
			case CLEANING : {
				return new String[] {CLEANING.name(), IMPORT.name(), SYNCH.name()};
			}
			case IMPORT : {
				return new String[] {CLEANING.name()};
			}
			case SYNCH : {
				return new String[] {CLEANING.name()};
			}
			}
			
			return new String[] {CLEANING.name(), IMPORT.name(), SYNCH.name()};
		}
	}
	
	private DataSource dataSource;
	private Integer maintenanceSemId = null;
	private Type semType;
	
	public MaintenanceSemaphore(Type semType) throws IOException {
		this.semType = semType;
		try {
			Context  context = new InitialContext();
			this.dataSource = (DataSource) context.lookup(CatmaDataSourceName.CATMADS.name());
			
			createAccess();
		}
		catch (NamingException ne) {
			throw new IOException(ne);
		}
	}

	private void createAccess() throws IOException {
		TransactionalDSLContext db = new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
		try {
			db.beginTransaction();

			Result<Record> otherExclusiveAccess = db
			.select()
			.from(MAINTENANCE_SEM)
			.where(MAINTENANCE_SEM.TYPE.in(semType.getExclusionTypes()))
			.forUpdate()
			.fetch();
			
			if (otherExclusiveAccess.isEmpty()) {
				maintenanceSemId = db
					.insertInto(MAINTENANCE_SEM, MAINTENANCE_SEM.TYPE)
					.values(semType.name())
					.returning(MAINTENANCE_SEM.MAINTENANCE_SEMID)
					.fetchOne()
					.map(new IDFieldToIntegerMapper(
						MAINTENANCE_SEM.MAINTENANCE_SEMID));
				Logger.getLogger(
						MaintenanceSemaphore.class.getName()).info(
							"aquired maintenance semaphore type " 
							+ semType + " id " + maintenanceSemId);
			}
			
			db.commitTransaction();
			
			if (maintenanceSemId == null) {
				Logger.getLogger(
					MaintenanceSemaphore.class.getName()).info(
						"aquiring maintenance semaphore type " 
						+ semType + " failed!");
			}
		}
		catch (Exception dae) {
			db.rollbackTransaction();
			db.close();
			throw new IOException(dae);
		}
		finally {
			if (db!=null) {
				db.close();
			}
		}
	}

	boolean hasAccess() {
		return maintenanceSemId != null;
	}
	
	public void release() {
		try {
			if (maintenanceSemId != null) {
				DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
				db
				.delete(MAINTENANCE_SEM)
				.where(MAINTENANCE_SEM.MAINTENANCE_SEMID.eq(maintenanceSemId))
				.execute();

				Logger.getLogger(
					MaintenanceSemaphore.class.getName()).info(
						"released maintenance semaphore type " 
						+ semType + " id " + maintenanceSemId);
						
				maintenanceSemId = null;
			}
		}
		catch (Exception e) {
			Logger.getLogger(MaintenanceSemaphore.class.getName()).log(
				Level.SEVERE, 
				"error releasing the maintenance semaphore type " 
				+ semType + " id " + maintenanceSemId);
		}
	}
}

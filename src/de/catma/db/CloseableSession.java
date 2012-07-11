package de.catma.db;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;

public class CloseableSession implements Closeable {
	
	private Session session;
	
	public CloseableSession(Session session) {
		this.session = session;
	}

	public CloseableSession(Session session, boolean rollback) {
		this(session);
		if (rollback) {
			try {
				if ((session != null) && (session.getTransaction().isActive())) {
					session.getTransaction().rollback();
				}
			}
			catch(Exception e){
				Logger.getLogger(
					this.getClass().getName()).log(
							Level.SEVERE, "Problem rolling back", e);
			}
		}
	}

	public void close() throws IOException {
		if ((session != null) && (session.isOpen())) {
			session.close();
		}
		session = null;
	}

}

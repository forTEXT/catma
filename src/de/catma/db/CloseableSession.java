package de.catma.db;

import java.io.Closeable;
import java.io.IOException;

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
			catch(Exception notOfInterest){}
		}
	}

	public void close() throws IOException {
		if ((session != null) && (session.isOpen())) {
			session.close();
		}
		session = null;
	}

}

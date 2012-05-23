package de.catma.repository.db;

import java.io.Closeable;
import java.io.IOException;

import org.hibernate.Session;

public class ClosableSession implements Closeable {
	
	private Session session;
	
	public ClosableSession(Session session) {
		this.session = session;
	}

	public void close() throws IOException {
		session.close();
		session = null;
	}

}

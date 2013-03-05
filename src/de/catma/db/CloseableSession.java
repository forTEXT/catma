/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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

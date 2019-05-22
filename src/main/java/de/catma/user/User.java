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
package de.catma.user;

import de.catma.rbac.RBACSubject;

/**
 * A user within the CATMA system.
 * 
 * @author marco.petris@web.de
 *
 */
public interface User extends RBACSubject {
	/**
	 * @return the numeric ID of the user
	 */
	Integer getUserId();

	/**
	 * @return an identifier like an email address
	 */
	String getIdentifier();

	/**
	 * @return the name of the user
	 */
	String getName();
	
	/**
	 * @return the email of the user
	 */
	String getEmail();

	/**
	 * @return true if the user is locked
	 */
	boolean isLocked();

	/**
	 * @return true if the user is a guest
	 */
	boolean isGuest();

	/**
	 * @return true if the user is spawnable
	 */
	boolean isSpawnable();
	
	/**
	 * @return true if the user has the given permission
	 */
	boolean hasPermission(Permission permission);
	
}

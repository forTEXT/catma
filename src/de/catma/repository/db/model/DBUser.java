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
package de.catma.repository.db.model;

import de.catma.user.Role;
import de.catma.user.User;

public class DBUser implements User {

	private Integer userId;
	private String identifier;
	private boolean locked;
	private Integer roleValue;
	
	public DBUser(Integer userId, String identifier, boolean locked, Role role) {
		this.userId = userId;
		this.identifier = identifier;
		this.locked = locked;
		this.roleValue = role.getVal();
	}

	public Integer getUserId() {
		return this.userId;
	}

	public String getIdentifier() {
		return this.identifier;
	}

	public boolean isLocked() {
		return this.locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}
	
	public String getName() {
		return identifier;
	}
	
	public Role getRole() {
		if (roleValue != null) {
			return Role.getRole(roleValue);
		}
		else {
			return Role.STANDARD;
		}
	}
}

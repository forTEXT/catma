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
package de.catma.repository.db;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.catma.user.Permission;
import de.catma.user.User;

public class DBUser implements User {

	private Integer userId;
	private String identifier;
	private boolean locked;
	private boolean guest;
	private boolean spawnable;
	private Set<String> permissions;
	
	public DBUser(Integer userId, String identifier, boolean locked, boolean guest, boolean spawnable) {
		this.userId = userId;
		this.identifier = identifier;
		this.locked = locked;
		this.guest = guest;
		this.spawnable = spawnable;
	}

	@Override
	public Integer getUserId() {
		return this.userId;
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}

	@Override
	public String getName() {
		return identifier;
	}

	@Override
	public String getEmail() {
		return this.identifier;
	}
	
	@Override
	public boolean isLocked() {
		return this.locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	@Override
	public boolean isGuest() {
		return guest;
	}
	
	@Override
	public boolean isSpawnable() {
		return spawnable;
	}

	@Override
	public boolean hasPermission(Permission permission) {
		return permissions.contains(permission.name());
	}

	void setPermissions(List<String> permissions) {
		this.permissions = new HashSet<>(permissions);
	}
}

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
package de.catma.tag;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A definition of a tag. That is a type of a {@link TagInstance}.
 * 
 * @author marco.petris@web.de
 */
public class TagDefinition implements Versionable {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	@Deprecated
	private Integer id;
	@Deprecated
	private Integer parentId;
	
	private String uuid;
	private String name;
	private Version version = new Version(); //TODO: replace version with createdDate
	private Map<String,PropertyDefinition> systemPropertyDefinitions;
	private Map<String,PropertyDefinition> userDefinedPropertyDefinitions;
	private Set<String> deletedPropertyDefinitions;
	private String parentUuid;
	private String tagsetDefinitionUuid;

	/**
	 * @param id the identifier of the definition (repository dependent)
	 * @param uuid a CATMA uuid see {@link de.catma.util.IDGenerator}
	 * @param name the name of the definition
	 * @param version the version of the definition
	 * @param parentId the identifier of the parent or <code>null</code> if this
	 * is already a top level definition
	 * @param parentUuid the CATMA uuid of the parent or <code>null</code> if this
	 * is already a top level definition
	 */
	public TagDefinition(
			Integer id, String uuid, 
			String name, Version version,  
			Integer parentId, String parentUuid, String tagsetDefinitionUuid) {
		this();
		this.id = id;
		this.uuid = uuid;
		this.name = name;
		this.version = version;
		this.parentId = parentId;
		this.parentUuid = parentUuid;
		if (this.parentUuid == null) {
			this.parentUuid = "";
		}
		this.tagsetDefinitionUuid = tagsetDefinitionUuid;
	}

	/**
	 * Copy constructor.
	 * @param toCopy
	 */
	public TagDefinition(TagDefinition toCopy) {
		this(null, toCopy.uuid, 
				toCopy.name, new Version(toCopy.version), 
				null, toCopy.parentUuid, toCopy.tagsetDefinitionUuid);
		
		for (PropertyDefinition pd : toCopy.getSystemPropertyDefinitions()) {
			addSystemPropertyDefinition(new PropertyDefinition(pd));
		}
		for (PropertyDefinition pd : toCopy.getUserDefinedPropertyDefinitions()) {
			addUserDefinedPropertyDefinition(new PropertyDefinition(pd));
		}	
	}

	public TagDefinition() {
		systemPropertyDefinitions = new HashMap<String, PropertyDefinition>();
		userDefinedPropertyDefinitions = new HashMap<String, PropertyDefinition>();
	}

	public Version getVersion() {
		return version;
	}

	/**
	 * Sets a new {@link Version}.
	 */
	void setVersion() {
		this.version = new Version();
	}
	
	
	@Override
	public String toString() {
		return "TAG_DEF[" + name 
				+ ",#"+id+"u#" + uuid +","
				+version
				+((parentUuid.isEmpty()) ? "]" : (",#"+parentUuid+"]"));
	}

	/**
	 * See {@link PropertyDefinition.SystemPropertyName} for possibilities.
	 * @param propertyDefinition
	 */
	public void addSystemPropertyDefinition(PropertyDefinition propertyDefinition) {
		systemPropertyDefinitions.put(propertyDefinition.getName(), propertyDefinition);
	}
	
	public void addUserDefinedPropertyDefinition(PropertyDefinition propertyDefinition) {
		userDefinedPropertyDefinitions.put(propertyDefinition.getUuid(), propertyDefinition);
	}	
	
	/**
	 * @return the CATMA uuid see {@link de.catma.util.IDGenerator}
	 */
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	/**
	 * @param name {@link #getName() name} of the PropertyDefinition
	 * @return the corresponding PropertyDefinition or <code>null</code> 
	 */
	public PropertyDefinition getPropertyDefinition(String name) {
		if (systemPropertyDefinitions.containsKey(name)) {
			return systemPropertyDefinitions.get(name);
		}
		else {
			return userDefinedPropertyDefinitions
					.values()
					.stream()
					.filter(pd -> pd.getName().equals(name))
					.findFirst()
					.orElse(null);
		}
	}
	
	/**
	 * @return non modifiable collection of user defined properties
	 */
	public Collection<PropertyDefinition> getUserDefinedPropertyDefinitions() {
		return Collections.unmodifiableCollection(userDefinedPropertyDefinitions.values());
	}
	
	/**
	 * @return the UUID of the parent TagDefinition or an empty String if this is
	 * 			a toplevel TagDefinittion. This method never returns <code>null</code>.
	 */
	public String getParentUuid() {
		return parentUuid;
	}

	public void setParentUuid(String uuid){
		this.parentUuid = uuid;
	}

	public String getTagsetDefinitionUuid() {
		return this.tagsetDefinitionUuid;
	}

	public void setTagsetDefinitionUuid(String tagsetDefinitionUuid) {
		this.tagsetDefinitionUuid = tagsetDefinitionUuid;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return see {@link PropertyDefinition.SystemPropertyName#catma_displaycolor}
	 */
	public String getColor() {
		return getPropertyDefinition(
			PropertyDefinition.SystemPropertyName.catma_displaycolor.name()).getFirstValue();
	}
	
	public String getAuthor() {
		PropertyDefinition authorPropertyDef =  getPropertyDefinition(
			PropertyDefinition.SystemPropertyName.catma_markupauthor.name());
		if (authorPropertyDef != null) {
			return authorPropertyDef.getFirstValue();
		}
		else {
			return null;
		}
	}

	public Collection<PropertyDefinition> getSystemPropertyDefinitions() {
		return Collections.unmodifiableCollection(
				systemPropertyDefinitions.values());
	}
	
	public void setColor(String colorAsRgbInt) {
		getPropertyDefinition(
			PropertyDefinition.SystemPropertyName.catma_displaycolor.name()).
				setValue(colorAsRgbInt);
	}
	
	void setAuthor(String author) {
		getPropertyDefinition(
			PropertyDefinition.SystemPropertyName.catma_markupauthor.name()).
				setValue(author);
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	/**
	 * @return repositoy dependent identifier
	 */
	public Integer getId() {
		return id;
	}
	
	public void setParentId(Integer parentId) {
		this.parentId = parentId;
	}
	
	/**
	 * @return the identifier of the parent or <code>null</code> if this is
	 * a toplevel definition
	 */
	public Integer getParentId() {
		return parentId;
	}

	/**
	 * Synchronizes this definition with the incoming definition. Deletions due 
	 * to the synch can be retrieved via {@link #getDeletedPropertyDefinitions()}.
	 * 
	 * @param other
	 * @param thisTagsetDefinition the tagset definition of this tag definition 
	 * is used to lookup the new {@link #getParentId() parent id} if the
	 * {@link #getParentUuid() uuid} of the parent has changed 
	 */
	@Deprecated
	void synchronizeWith(TagDefinition other, TagsetDefinition thisTagsetDefinition) {
		if (!this.getVersion().equals(other.getVersion())) {
			this.name = other.name;
			this.version = new Version(other.getVersion());
			if (!this.parentUuid.equals(other.uuid)) {
				this.parentId = null;
			}
			this.parentUuid = other.parentUuid;
			if (!parentUuid.isEmpty()) {
				TagDefinition parentDefinition = 
						thisTagsetDefinition.getTagDefinition(this.parentUuid);
				if (parentDefinition != null) {
					this.parentId = parentDefinition.getId();
				}
			}
			
			synchSystemPropertyDefinitions(systemPropertyDefinitions.values(), other);
			synchPropertyDefinitions(userDefinedPropertyDefinitions.values(), other);

			for (PropertyDefinition pd : other.getSystemPropertyDefinitions()) {
				if (this.getPropertyDefinition(pd.getName()) == null) {
					logger.info("adding system property " + pd + " to " + this + " because of synch");
					addSystemPropertyDefinition(
							new PropertyDefinition(pd));
				}
			}

			for (PropertyDefinition pd : other.getUserDefinedPropertyDefinitions()) {
				if (this.getPropertyDefinition(pd.getName()) == null) {
					logger.info("adding user property " + pd + " to " + this + " because of synch");
					addUserDefinedPropertyDefinition(
							new PropertyDefinition(pd));
				}
			}
			
		}
	}

	/**
	 * Synchs the given property definitions of this TagDefinition with the 
	 * property definitions of the other TagDefinition  
	 * @param propertyDefinitions
	 * @param other
	 */
	private void synchPropertyDefinitions(
			Collection<PropertyDefinition> propertyDefinitions,
			TagDefinition other) {
		
		Iterator<PropertyDefinition> pdIterator =
				propertyDefinitions.iterator();
		
		while (pdIterator.hasNext()) {
			PropertyDefinition pd  = pdIterator.next();
			PropertyDefinition otherPd = other.getPropertyDefinition(pd.getName());
			
			if (otherPd != null) {
				logger.info("synching " + pd + " with "  + otherPd);
				pd.synchronizeWith(otherPd);
			}
			else {
				logger.info("deleting " + pd + " from " + this);
				pdIterator.remove();
			}
		}	
	}
	
	private void synchSystemPropertyDefinitions(
			Collection<PropertyDefinition> propertyDefinitions,
			TagDefinition other) {
		
		Iterator<PropertyDefinition> pdIterator =
				propertyDefinitions.iterator();
		
		while (pdIterator.hasNext()) {
			PropertyDefinition pd  = pdIterator.next();
			PropertyDefinition otherPd = other.getPropertyDefinition(pd.getName());
			
			if (otherPd != null) {
				logger.info("synching " + pd + " with "  + otherPd);
				pd.synchronizeWith(otherPd);
			}
			else {
				logger.info("deleting " + pd + " from " + this);
				pdIterator.remove();
			}
		}	
	}

	public void removeUserDefinedPropertyDefinition(PropertyDefinition propertyDefinition) {
		this.userDefinedPropertyDefinitions.remove(propertyDefinition.getUuid());
	}

	public PropertyDefinition getPropertyDefinitionByUuid(String uuid) {
		return this.userDefinedPropertyDefinitions.get(uuid);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TagDefinition other = (TagDefinition) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}
	
	
}

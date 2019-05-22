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
import java.util.HashSet;
import java.util.Map;

import de.catma.tag.PropertyDefinition.SystemPropertyName;
import de.catma.util.IDGenerator;

/**
 * An instance of a tag. The TagInstance has a {@link TagDefinition type}, a
 * set of user defined {@link Property properties} and a set of system properties.
 * 
 * @author marco.petris@web.de
 *
 */
public class TagInstance {
	private String uuid;
	private String tagDefinitionId;
	private Map<String,Property> userDefinedProperties;
	private String tagsetId;
	private String author;
	private String timestamp;
	
	/**
	 * System properties get the {@link PropertyDefinition#getFirstValue() default} value set.
	 * @param uuid CATMA uuid of the instance (see {@link de.catma.util.IDGenerator}.
	 * @param tagDefinition the type of this instance
	 */
	public TagInstance(
			String uuid, String tagDefinitionId, 
			String author, String timestamp,
			Collection<PropertyDefinition> userDefinedPropertyDefs,
			String tagsetId) {
		
		this.uuid = uuid;
		this.tagDefinitionId = tagDefinitionId;
		this.author = author;
		this.timestamp = timestamp;
		
		userDefinedProperties = new HashMap<String, Property>();
		
		for (PropertyDefinition pDef : userDefinedPropertyDefs) {
			userDefinedProperties.put(
				pDef.getUuid(), 
				new Property(
					pDef.getUuid(),
					Collections.<String>emptySet()));
		}
		this.tagsetId = tagsetId;
	}
	


	public String getTagDefinitionId() {
		return tagDefinitionId;
	}

	public void addSystemProperty(Property property) {
		String propertyDefinitionId = property.getPropertyDefinitionId();
		IDGenerator idGenerator = new IDGenerator();
		
		String authorId = idGenerator.generate(SystemPropertyName.catma_markupauthor.name());
		String timestampId = idGenerator.generate(SystemPropertyName.catma_markuptimestamp.name());
		
		if (authorId.equals(propertyDefinitionId)) {
			this.author = property.getFirstValue();
		}
		else if (timestampId.equals(propertyDefinitionId)) {
			this.timestamp = property.getFirstValue();
		}
	}
	
	public void addUserDefinedProperty(Property property) {
		userDefinedProperties.put(property.getPropertyDefinitionId(), property);
	}
	
	@Override
	public String toString() {
		return "TAGINSTANCE[#"+uuid+",TAG#"+tagDefinitionId+"]";
	}
	
	/**
	 * @return CATMA uuid of the instance (see {@link de.catma.util.IDGenerator}
	 */
	public String getUuid() {
		return uuid;
	}
	
	public Property getUserDefinedPropetyByUuid(String uuid) {
		return userDefinedProperties.get(uuid);
	}
	
	/**
	 * @return non modifiable collection of system properties 
	 * @see PropertyDefinition.SystemPropertyName
	 */
	public Collection<Property> getSystemProperties() {
		Collection<Property> systemProperties = new HashSet<>();
		IDGenerator idGenerator = new IDGenerator();
		
		String authorId = idGenerator.generate(SystemPropertyName.catma_markupauthor.name());
		systemProperties.add(new Property(authorId, Collections.singleton(author)));
		String timestampId = idGenerator.generate(SystemPropertyName.catma_markuptimestamp.name());
		systemProperties.add(new Property(timestampId, Collections.singleton(timestamp)));
		
		return Collections.unmodifiableCollection(systemProperties);
	}
	
	/**
	 * @return non modifiable list of user defined properties
	 */
	public Collection<Property> getUserDefinedProperties() {
		return Collections.unmodifiableCollection(userDefinedProperties.values());
	}
	
	/**
	 * Sychnronizes the properties of this instance which the attached 
	 * {@link PropertyDefinition}, property values don't get overridden 
	 */
	@Deprecated
	public void synchronizeProperties() {
		
//		Iterator<Map.Entry<String, Property>> iterator = systemProperties.entrySet().iterator();
//		while (iterator.hasNext()) {
//			Map.Entry<String, Property> entry = iterator.next();
//			Property p = entry.getValue();
////			if (getTagDefinition().getPropertyDefinition(entry.getKey())==null) {
////				iterator.remove();
////			}
////			else {
////				p.synchronize();
////			}
//		}
		
		// we do not update Property values, therefore we handle only ...
		
		// ... deletion and ...
//		iterator = userDefinedProperties.entrySet().iterator();
//		while (iterator.hasNext()) {
//			Map.Entry<String, Property> entry = iterator.next();
//			
////			if (getTagDefinition().getPropertyDefinition(entry.getKey())==null) {
////				iterator.remove();
////			}
//		}
//		
////		// ... addition of properties
////		for (PropertyDefinition pd : getTagDefinition().getUserDefinedPropertyDefinitions()) {
////			if (!userDefinedProperties.containsKey(pd.getUuid())) {
//////				addUserDefinedProperty(new Property(pd, Collections.<String>emptySet()));
////			}
////		}
	}
	
	public void removeUserDefinedProperty(String propertyDefId) {
		this.userDefinedProperties.remove(propertyDefId);
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
		TagInstance other = (TagInstance) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

	public String getAuthor() {
		return author;
	}

	public String getTimestamp() {
		return timestamp;
	}
	
	public String getTagsetId() {
		return tagsetId;
	}

	public void setAuthor(String author) {
		this.author = author;	
	}
	
}

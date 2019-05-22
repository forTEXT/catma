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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import de.catma.document.source.ContentInfoSet;
import de.catma.util.Pair;

/**
 * This class bundles operations upon {@link TagLibary TagLibraries} and its
 * content.
 * 
 * @author marco.petris@web.de
 *
 */
public class TagManager {
	
	/**
	 * Events issued by this manager.
	 */
	public enum TagManagerEvent {
		/**
		 * <p>{@link PropertyDefinition} added:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = a {@link Pair} of the new 
		 * {@link PropertyDefinition} and its {@link TagDefinition}</li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = <code>null</code></li>
		 * </p><br />
		 * <p>{@link PropertyDefinition} removed:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = <code>null</code></li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = a {@link Pair} of the 
		 * removed {@link PropertyDefinition} and with a Pair of its {@link TagDefinition} and {@link TagsetDefinition}</li>
		 * </p><br />
		 * <p>{@link PropertyDefinition} changed:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = the changed {@link PropertyDefinition}</li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = the {@link TagDefinition}</li>
		 * </p>
		 */
		userPropertyDefinitionChanged,
		/**
		 * <p>{@link TagsetDefinition} added:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = a new {@link TagsetDefinition} 
		 * <li>{@link PropertyChangeEvent#getOldValue()} = <code>null</code></li>
		 * </p><br />
		 * <p>{@link TagsetDefinition} removed:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = <code>null</code></li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = the  removed {@link TagsetDefinition}
		 * </p><br />
		 * <p>{@link TagsetDefinition} changed:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = the changed {@link TagsetDefinition}</li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = the old name</li>
		 * </p>
		 */
		tagsetDefinitionChanged,
		/**
		 * <p>{@link TagDefinition} added:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = a {@link Pair} of the new 
		 * {@link TagDefinition} and its {@link TagsetDefinition}Pair&lt;TagsetDefinition,TagDefinition&gt;</li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = <code>null</code></li>
		 * </p><br />
		 * <p>{@link TagDefinition} removed:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = <code>null</code></li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = a {@link Pair} of the 
		 * removed {@link TagDefinition} and its {@link TagsetDefinition} Pair&lt;TagsetDefinition,TagDefinition&gt;</li>
		 * </p><br />
		 * <p>{@link TagDefinition} changed:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = the modified {@link TagDefinition}</li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = the {@link TagsetDefinition}</li>
		 */
		tagDefinitionChanged,
		/**
		 * <p>{@link TagLibrary} added:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = the new library</li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = <code>null</code></li>
		 * </p><br />
		 * <p>{@link TagLibrary} removed:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = <code>null</code></li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = the removed library</li>
		 * </p><br />
		 * <p>{@link TagLibrary} changed:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = the changed {@link TagLibrary}</li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = the {@link ContentInfoSet old bibliographical data }</li>
		 */
		tagLibraryChanged, 
		;
	}
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private TagLibrary tagLibrary;
	
	private PropertyChangeSupport propertyChangeSupport;
	
	public TagManager(TagLibrary tagLibrary) {
		this.tagLibrary = tagLibrary;
		this.propertyChangeSupport = new PropertyChangeSupport(this);
	}
	
	public void load(Collection<TagsetDefinition> tagsets) {
		this.tagLibrary.clear();
		tagsets.forEach(tagset ->this.tagLibrary.add(tagset));
	}
	
	public void addTagsetDefinition(
			TagsetDefinition tagsetDefinition) {
		tagLibrary.add(tagsetDefinition);
		this.propertyChangeSupport.firePropertyChange(
			TagManagerEvent.tagsetDefinitionChanged.name(),
			null, 
			tagsetDefinition);
	}

	public void addPropertyChangeListener(TagManagerEvent propertyName,
			PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(
				propertyName.name(), listener);
	}

	public void removePropertyChangeListener(TagManagerEvent propertyName,
			PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(propertyName.name(),
				listener);
	}

	public void setTagsetDefinitionName(
			TagsetDefinition tagsetDefinition, String name) {
		String oldName = tagsetDefinition.getName();
		tagsetDefinition.setName(name);
		tagsetDefinition.setVersion();
		this.propertyChangeSupport.firePropertyChange(
				TagManagerEvent.tagsetDefinitionChanged.name(),
				oldName,
				tagsetDefinition);
	}

	public void removeTagsetDefinition(
			TagsetDefinition tagsetDefinition) {
		tagLibrary.remove(tagsetDefinition);
		this.propertyChangeSupport.firePropertyChange(
				TagManagerEvent.tagsetDefinitionChanged.name(),
				tagsetDefinition,
				null);
	}

	public void addTagDefinition(TagsetDefinition tagsetDefinition,
			TagDefinition tagDefinition) {
		tagsetDefinition.addTagDefinition(tagDefinition);
		tagsetDefinition.setVersion();
		this.propertyChangeSupport.firePropertyChange(
			TagManagerEvent.tagDefinitionChanged.name(),
			null,
			new Pair<TagsetDefinition, TagDefinition>(
					tagsetDefinition, tagDefinition));
	}

	public void removeTagDefinition(TagsetDefinition tagsetDefinition,
			TagDefinition tagDefinition) {
		tagsetDefinition.remove(tagDefinition);
		tagsetDefinition.setVersion();
		this.propertyChangeSupport.firePropertyChange(
				TagManagerEvent.tagDefinitionChanged.name(),
				new Pair<TagsetDefinition, TagDefinition>(tagsetDefinition, tagDefinition),
				null);
	}
	
	public void updateTagDefinition(TagDefinition tag, TagDefinition updatedTag) {
		TagsetDefinition tagset = this.tagLibrary.getTagsetDefinition(tag);
		
		for (PropertyDefinition pd : new ArrayList<>(tag.getUserDefinedPropertyDefinitions())) {
			if (!updatedTag.getUserDefinedPropertyDefinitions().contains(pd)) {
				removeUserDefinedPropertyDefinition(pd, tag, tagset);
			}
		}
		
		for (PropertyDefinition pd : updatedTag.getUserDefinedPropertyDefinitions()) {
			if (!tag.getUserDefinedPropertyDefinitions().contains(pd)) {
				tag.addUserDefinedPropertyDefinition(new PropertyDefinition(pd));
			}
			else {
				tag.getPropertyDefinitionByUuid(
					pd.getUuid()).setPossibleValueList(pd.getPossibleValueList());
			}
		}
		
		tag.setName(updatedTag.getName());
		tag.setColor(updatedTag.getColor());
		
		this.propertyChangeSupport.firePropertyChange(
				TagManagerEvent.tagDefinitionChanged.name(),
				tagset,
				tag);
	}

	/**
	 * Synchronizes td1 with td2 via {@link TagsetDefinition#synchronizeWith(TagsetDefinition)}}
	 * @param td1
	 * @param td2
	 */
	@Deprecated
	public void synchronize(TagsetDefinition td1, TagsetDefinition td2) {
		logger.info("synching " + td1 + " with " + td2);
		td1.synchronizeWith(td2);
		// no event needed, since synchronization goes always along with
		// modifications of UserMarkupCollections, we handle the two
		// things together in the UserMarkupCollectionManager
	}

	public void removeUserDefinedPropertyDefinition(
			PropertyDefinition propertyDefinition, TagDefinition tagDefinition, TagsetDefinition tagsetDefinition) {
		tagsetDefinition.remove(propertyDefinition, tagDefinition);
		tagDefinition.setVersion();
		this.propertyChangeSupport.firePropertyChange(
				TagManagerEvent.userPropertyDefinitionChanged.name(),
				new Pair<>(
						propertyDefinition, new Pair<>(tagDefinition, tagsetDefinition)),
				null);
	}
	
	public void updateTagLibrary(
			TagLibraryReference tagLibraryReference, ContentInfoSet contentInfoSet) {
		ContentInfoSet oldContentInfoSet = tagLibraryReference.getContentInfoSet();
		tagLibraryReference.setContentInfoSet(contentInfoSet);
		
		this.propertyChangeSupport.firePropertyChange(
			TagManagerEvent.tagLibraryChanged.name(),
			oldContentInfoSet,
			tagLibraryReference);
	}

	public void addUserDefinedPropertyDefinition(TagDefinition td,
			PropertyDefinition propertyDefinition) {
		td.addUserDefinedPropertyDefinition(propertyDefinition);
		td.setVersion();
		this.propertyChangeSupport.firePropertyChange(
				TagManagerEvent.userPropertyDefinitionChanged.name(),
				null,
				new Pair<PropertyDefinition, TagDefinition>(propertyDefinition, td));
	}

	public void updateUserDefinedPropertyDefinition(
			TagDefinition td,
			PropertyDefinition propertyDefinition) {
		td.setVersion();
		this.propertyChangeSupport.firePropertyChange(
				TagManagerEvent.userPropertyDefinitionChanged.name(),
				td,
				propertyDefinition);
	}

	public TagLibrary getTagLibrary() {
		return tagLibrary;
	}
}

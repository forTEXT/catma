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
package de.catma.document.repository;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.tag.TagManager;


/**
 * This class handles open/close requests of {@link Repository repositories).
 * 
 * @author marco.petris@web.de
 *
 */
@Deprecated
public class RepositoryManager {
	/**
	 * Events emitted by the RepositoryManager.
	 * 
	 * @see RepositoryManager#addPropertyChangeListener(RepositoryManagerEvent, PropertyChangeListener)
	 */
	public static enum RepositoryManagerEvent {
		/**
		 * <p>{@link Repository} opened:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = {@link Repository}</li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = <code>null</code></li>
		 * </p><br />
		 * <p>{@link Repository} closed:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = <code>null</code></li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = {@link Repository}</li>
		 * </p><br />
		 */
		repositoryStateChange,
		;
	}
	private BackgroundServiceProvider backgroundServiceProvider;
	private TagManager tagManager;

	private Set<RepositoryReference> repositoryReferences;
	private Set<Repository> openRepositories;
	private PropertyChangeSupport propertyChangeSupport;
	
	/**
	 * Creates a repository factory for each repository specified in the properties.
	 * @param backgroundServiceProvider used for creating a {@link RepositoryFactory#createRepository(BackgroundServiceProvider, TagManager, Properties, int) Repository}.
	 * @param tagManager used for creating a {@link RepositoryFactory#createRepository(BackgroundServiceProvider, TagManager, Properties, int) Repository}.
	 * @param properties used for creating a {@link RepositoryFactory#createRepository(BackgroundServiceProvider, TagManager, Properties, int) Repository}.
	 * @throws Exception error creating repositories
	 */
	public RepositoryManager(
			BackgroundServiceProvider backgroundServiceProvider, 
			TagManager tagManager, Properties properties) throws Exception {
		propertyChangeSupport = new PropertyChangeSupport(this);
		this.backgroundServiceProvider = backgroundServiceProvider;
		this.tagManager = tagManager;
		
		repositoryReferences = new TreeSet<RepositoryReference>(
				new Comparator<RepositoryReference>() {
			@Override
			public int compare(RepositoryReference o1, RepositoryReference o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		
		openRepositories = new HashSet<Repository>();
		
		int index=1;
		while(RepositoryPropertyKey.Repository.exists(properties, index)) {
			
			RepositoryFactory repositoryFactory =  
				(RepositoryFactory)Class.forName(
					RepositoryPropertyKey.RepositoryFactory.getProperty(properties, index),
					true, Thread.currentThread().getContextClassLoader()).newInstance();
			
			repositoryReferences.add(
					new RepositoryReference(
						repositoryFactory, properties, index));
			
			index++;
		}

	}
	
	

	/**
	 * @see RepositoryManagerEvent
	 * @see PropertyChangeSupport#addPropertyChangeListener(String, PropertyChangeListener)
	 */
	public void addPropertyChangeListener(RepositoryManagerEvent propertyName,
			PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(propertyName.name(), listener);
	}



	/**
	 * @see RepositoryManagerEvent
	 * @see PropertyChangeSupport#removePropertyChangeListener(String, PropertyChangeListener)
	 */
	public void removePropertyChangeListener(RepositoryManagerEvent propertyName,
			PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(propertyName.name(),
				listener);
	}



	/**
	 * @return all possible, i. e. configured, repositories
	 */
	public Set<RepositoryReference> getRepositoryReferences() {
		return Collections.unmodifiableSet(repositoryReferences);
	}

	/**
	 * @param repositoryReference the repository configuration to open
	 * @param userIdentification a key/value store with items for user identification
	 * @return the opened repository
	 * @throws Exception
	 */
	public Repository openRepository(
			RepositoryReference repositoryReference, 
			Map<String, String> userIdentification) throws Exception {
		Repository repository = 
				repositoryReference.getRepositoryFactory().createRepository(
				backgroundServiceProvider, tagManager,
				repositoryReference.getProperties(),
				repositoryReference.getIndex());
		
//		repository.open(userIdentification);
		
		openRepositories.add(repository);
		propertyChangeSupport.firePropertyChange(
				RepositoryManagerEvent.repositoryStateChange.name(), null, repository);
		return repository;
	}
	
	/**
	 * @param repository the repo to be closed
	 */
	public void close(Repository repository) {
		openRepositories.remove(repository);
		repository.close();
		propertyChangeSupport.firePropertyChange(
				RepositoryManagerEvent.repositoryStateChange.name(), repository, null);
	}
	
	/**
	 * Close all open repositories.
	 */
	public void close() {
		for (Repository r : openRepositories) {
			try {
				r.close();
			}
			catch (Throwable t) {
				Logger.getLogger(getClass().getName()).log(
						Level.SEVERE, "error closing repository " + r, t);
			}
		}
		openRepositories.clear();
		repositoryReferences.clear();
	}

	/**
	 * @param repositoryReference
	 * @return true if there is an open repository for the given configuration
	 */
	public boolean isOpen(RepositoryReference repositoryReference) {
		for (Repository r : openRepositories) {
			if (r.getName().equals(repositoryReference.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return true if ther is any open repository.
	 */
	public boolean hasOpenRepository() {
		return !openRepositories.isEmpty();
	}
	
	public Repository getFirstOpenRepository() {
		if (hasOpenRepository()) {
			return openRepositories.iterator().next();
		}
		else {
			return null;
		}
	}
}

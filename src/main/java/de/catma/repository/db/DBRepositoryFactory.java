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

import java.util.Properties;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryFactory;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.indexer.IndexerFactory;
import de.catma.serialization.SerializationHandlerFactory;
import de.catma.tag.TagManager;

public class DBRepositoryFactory implements RepositoryFactory {

	public Repository createRepository(BackgroundServiceProvider backgroundServiceProvider,
			TagManager tagManager, Properties properties, int index)
			throws Exception {
		
		String indexerFactoryClassName = 
				RepositoryPropertyKey.IndexerFactory.getProperty(properties, index);
		
		IndexerFactory indexerFactory = 
				(IndexerFactory)Class.forName(
						indexerFactoryClassName, true,
						Repository.class.getClassLoader()).newInstance();

		String serializationHandlerFactoryClazzName = 
				RepositoryPropertyKey.SerializationHandlerFactory.getProperty(properties, index);
		SerializationHandlerFactory serializationHandlerFactory = 
				(SerializationHandlerFactory) Class.forName(
						serializationHandlerFactoryClazzName, true, 
						Repository.class.getClassLoader()).newInstance();
		serializationHandlerFactory.setTagManager(tagManager);

		return new DBRepository(
			RepositoryPropertyKey.Repository.getProperty(properties, index),
			RepositoryPropertyKey.RepositoryFolderPath.getProperty(properties, index),
			RepositoryPropertyKey.RepositoryAuthenticationRequired.isTrue(properties, index, false),
			tagManager,
			backgroundServiceProvider,
			indexerFactory,
			serializationHandlerFactory,
			RepositoryPropertyKey.RepositoryUrl.getProperty(properties, index),
			RepositoryPropertyKey.RepositoryUser.getProperty(properties, index),
			RepositoryPropertyKey.RepositoryPass.getProperty(properties, index),
			properties.getProperty(RepositoryPropertyKey.TempDir.name()));
	}

}

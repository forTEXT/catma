package de.catma.repository.db;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.Session;

import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.repository.db.model.DBProperty;
import de.catma.repository.db.model.DBPropertyDefinition;
import de.catma.repository.db.model.DBPropertyValue;
import de.catma.repository.db.model.DBTagDefinition;
import de.catma.repository.db.model.DBTagInstance;
import de.catma.repository.db.model.DBTagReference;
import de.catma.repository.db.model.DBUserMarkupCollection;
import de.catma.tag.Property;
import de.catma.tag.TagInstance;
import de.catma.util.IDGenerator;

public class DBUserMarkupCollectionUpdater {

	private IDGenerator idGenerator;

	public DBUserMarkupCollectionUpdater(DBRepository dbRepository) {
		this.idGenerator = new IDGenerator();
	}

	public void updateUserMarkupCollection(Session session,
			UserMarkupCollection userMarkupCollection) {

		DBUserMarkupCollection dbUserMarkupCollection =
				(DBUserMarkupCollection) session.load(
						DBUserMarkupCollection.class, 
						Integer.valueOf(userMarkupCollection.getId()));

		Map<String, DBTagInstance> persistentTagInstances = 
				new HashMap<String,DBTagInstance>();
		
		for (DBTagReference dbTr : dbUserMarkupCollection.getDbTagReferences()) {
			persistentTagInstances.put(
				idGenerator.uuidBytesToCatmaID(dbTr.getDbTagInstance().getUuid()),
				dbTr.getDbTagInstance());
		}
		
		Map<String, TagInstance> incomingTagInstances = 
				new HashMap<String, TagInstance>();
		
		for (TagReference tr : userMarkupCollection.getTagReferences()) {
			incomingTagInstances.put(tr.getTagInstanceID(), tr.getTagInstance());
		}
		
		Iterator<Map.Entry<String, DBTagInstance>> iterator = 
				persistentTagInstances.entrySet().iterator();
		while (iterator.hasNext()) {
			DBTagInstance dbTagInstance = iterator.next().getValue();
			if (!incomingTagInstances.containsKey(
				idGenerator.uuidBytesToCatmaID(dbTagInstance.getUuid()))) {
				dbUserMarkupCollection.getDbTagReferences().removeAll(
						dbTagInstance.getDbTagReferences());
				session.delete(dbTagInstance);
				iterator.remove();
			}
			else {
				updateDbTagIntance(
					session, dbTagInstance, 
					incomingTagInstances.get(
						idGenerator.uuidBytesToCatmaID(dbTagInstance.getUuid())));
				session.saveOrUpdate(dbTagInstance);
			}
		}
		
		for (TagInstance tagInstance : incomingTagInstances.values()) {
			if (!persistentTagInstances.containsKey(tagInstance.getUuid())) {
				createDbTagInstance(session, tagInstance);
			}
		}
	}
	
	private void createDbTagInstance(
			Session session, TagInstance tagInstance) {
		DBTagDefinition dbTagDefinition = 
				(DBTagDefinition) session.load(
					DBTagDefinition.class, 
					tagInstance.getTagDefinition().getId());
		DBTagInstance dbTagInstance = 
				new DBTagInstance(
					idGenerator.catmaIDToUUIDBytes(tagInstance.getUuid()), 
					dbTagDefinition);
		for (Property prop : tagInstance.getUserDefinedProperties()) {
			createDbProperty(session, prop, dbTagInstance);
		}
		
		session.save(dbTagInstance);
	}
	
	private void createDbProperty(Session session, Property prop, DBTagInstance dbTagInstance) {
		DBPropertyDefinition dbPropertyDefinition = 
				(DBPropertyDefinition) session.load(
					DBPropertyDefinition.class, 
					prop.getPropertyDefinition().getId());
		DBProperty dbProperty = 
				new DBProperty(dbPropertyDefinition, dbTagInstance);
		dbTagInstance.getDbProperties().add(dbProperty);
		
		for (String value : prop.getPropertyValueList().getValues()) {
			dbProperty.getDbPropertyValues().add(
				new DBPropertyValue(dbProperty, value));
		}

	}

	private void updateDbTagIntance(Session session, DBTagInstance dbTagInstance,
			TagInstance tagInstance) {
		Iterator<DBProperty> dbPropertyIterator = 
				dbTagInstance.getDbProperties().iterator();
		while (dbPropertyIterator.hasNext()) {
			DBProperty dbProperty = dbPropertyIterator.next();
			if (tagInstance.getProperty(
					idGenerator.uuidBytesToCatmaID(
							dbProperty.getDbPropertyDefinition().getUuid())) == null) {
				dbPropertyIterator.remove();
				session.delete(dbProperty);
			}
			else {
				updateDbProperty(
					session, dbProperty,
					tagInstance.getProperty(
							idGenerator.uuidBytesToCatmaID(
									dbProperty.getDbPropertyDefinition().getUuid())));
			}
		}
	}

	void updateDbProperty(Session session, DBProperty dbProperty, Property property) {
		Iterator<DBPropertyValue> dbPropertyValueIterator = 
				dbProperty.getDbPropertyValues().iterator();
		while (dbPropertyValueIterator.hasNext()) {
			DBPropertyValue dbPropertyValue = dbPropertyValueIterator.next();
			if (!property.getPropertyValueList().getValues().contains(dbPropertyValue.getValue())) {
				dbPropertyValueIterator.remove();
				session.delete(dbPropertyValue);
			}
		}
		
		for (String value : property.getPropertyValueList().getValues()) {
			if (!dbProperty.hasPropertyValue(value)) {
				DBPropertyValue newValue = new DBPropertyValue(dbProperty, value);
				dbProperty.getDbPropertyValues().add(newValue);
			}
		}
	}
}

package de.catma.ui;

/**
 * A key value storage
 * @author db
 *
 */
public interface KeyValueStorage {

	/**
	 * stores an object at a specified key 
	 * @param key
	 * @param obj
	 * @return
	 */
	public Object setAttribute(String key, Object obj);
	
	/**
	 * retrieves an object stored at a specified key
	 * @param key
	 * @return
	 */
	public Object getAttribute(String key);
	
		
}

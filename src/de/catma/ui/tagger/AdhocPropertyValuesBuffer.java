package de.catma.ui.tagger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * A set of instance values which will appear in a combo box.
 * @author alexandra.krah@googlemail.com
 *
 */
public class AdhocPropertyValuesBuffer {
	
	private Set<String> instanceValues;
	
	public AdhocPropertyValuesBuffer(){
		instanceValues = new HashSet<String>();
	}
	
	void addValue(String value){
		if (!instanceValues.contains(value)) {
			instanceValues.add(value);
		}
	}
	
	void remove(String value){
		instanceValues.remove(value);
	}
	
	/**
	 * @return non modifiable set of the instance values
	 */	
	public Set<String> getValues(){
		return Collections.unmodifiableSet(instanceValues);
	}
	
}

package de.catma.repository.git.serialization.model_wrappers;

import com.jsoniter.annotation.JsonIgnore;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyPossibleValueList;

import java.util.List;

public class GitPropertyDefinition {
	private PropertyDefinition propertyDefinition;

	public GitPropertyDefinition(){
		this.propertyDefinition = new PropertyDefinition();
	}

	public GitPropertyDefinition(PropertyDefinition propertyDefinition){
		this.propertyDefinition = propertyDefinition;
	}

	@JsonIgnore
	public PropertyDefinition getPropertyDefinition() {
		return this.propertyDefinition;
	}

//	private Integer id;
//	private String name;
//	private String uuid;
//	private PropertyPossibleValueList possibleValueList;

	public String getUuid(){return this.propertyDefinition.getUuid();}

	public void setUuid(String uuid){this.propertyDefinition.setUuid(uuid);}

	public String getName(){return this.propertyDefinition.getName();}

	public void setName(String name){this.propertyDefinition.setName(name);}

	// TODO: how to serialize singleSelect property?
	public List<String> getPossibleValueList(){
		return this.propertyDefinition.getPossibleValueList().getPropertyValueList().getValues();
	}

	public void setPossibleValueList(List<String> values){
		this.propertyDefinition.setPossibleValueList(new PropertyPossibleValueList(values, true));
	}

}

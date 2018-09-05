package de.catma.repository.git.serialization.model_wrappers;

import java.util.List;

import com.jsoniter.annotation.JsonIgnore;

import de.catma.tag.PropertyDefinition;

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

	public String getName(){return this.propertyDefinition.getName();}

	public void setName(String name){this.propertyDefinition.setName(name);}

	public List<String> getPossibleValueList(){
		return this.propertyDefinition.getPossibleValueList();
	}

	public void setPossibleValueList(List<String> values){
	}

}

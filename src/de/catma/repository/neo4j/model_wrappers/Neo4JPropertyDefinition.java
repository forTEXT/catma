package de.catma.repository.neo4j.model_wrappers;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyPossibleValueList;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import java.util.ArrayList;
import java.util.List;

@NodeEntity(label="PropertyDefinition")
public class Neo4JPropertyDefinition {
	@Id
	@GeneratedValue
	private Long id;

	private String uuid;
	private String name;
	private boolean singleSelect;
	private List<String> possibleValues;

	public Neo4JPropertyDefinition(){
		this.singleSelect = true;
		this.possibleValues = new ArrayList<>();
	}

	public Neo4JPropertyDefinition(PropertyDefinition propertyDefinition){
		this();

		this.setPropertyDefinition(propertyDefinition);
	}

	public Long getId() {
		return this.id;
	}

	public String getUuid() {
		return this.uuid;
	}

	public String getName() {
		return this.name;
	}

	public boolean isSingleSelect() {
		return this.singleSelect;
	}

	public List<String> getPossibleValues() {
		return this.possibleValues;
	}

	public PropertyDefinition getPropertyDefinition(){
		PropertyDefinition propertyDefinition = new PropertyDefinition();

		propertyDefinition.setUuid(this.uuid);
		propertyDefinition.setName(this.name);
		propertyDefinition.setPossibleValueList(new PropertyPossibleValueList(this.possibleValues, this.singleSelect));

		return propertyDefinition;
	}

	public void setPropertyDefinition(PropertyDefinition propertyDefinition){
		this.uuid = propertyDefinition.getUuid();
		this.name = propertyDefinition.getName();
		this.singleSelect = propertyDefinition.getPossibleValueList().isSingleSelect();

		this.possibleValues.addAll(propertyDefinition.getPossibleValueList().getPropertyValueList().getValues());
	}
}

package de.catma.repository.neo4j.serialization.model_wrappers;

import de.catma.tag.Property;
import de.catma.tag.PropertyValueList;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

@NodeEntity(label="Property")
public class Neo4JProperty {
	@Id
	@GeneratedValue
	private Long id;

	@Relationship(type="HAS_PROPERTY_DEFINITION", direction=Relationship.OUTGOING)
	private Neo4JPropertyDefinition propertyDefinition;

	private List<String> values;

	public Neo4JProperty() {
		this.values = new ArrayList<>();
	}

	public Neo4JProperty(Property property) {
		this();

		this.setProperty(property);
	}

	public Property getProperty() {
		return new Property(this.propertyDefinition.getPropertyDefinition(), new PropertyValueList(this.values));
	}

	public void setProperty(Property property) {
		this.propertyDefinition = new Neo4JPropertyDefinition(property.getPropertyDefinition());
		this.values = property.getPropertyValueList().getValues();
	}
}

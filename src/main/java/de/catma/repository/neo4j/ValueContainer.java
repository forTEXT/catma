package de.catma.repository.neo4j;

public class ValueContainer<T> {
	private T value;
	
	public ValueContainer() {
	}

	public ValueContainer(T value) {
		super();
		this.value = value;
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}
}

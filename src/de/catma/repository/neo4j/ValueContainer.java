package de.catma.repository.neo4j;

public class ValueContainer<T> {
	private T value;

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}
}

package de.catma.repository.db.executionshield;

public interface DBOperation<T> {
	public T execute() throws Exception;
}

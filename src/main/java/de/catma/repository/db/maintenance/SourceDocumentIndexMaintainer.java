package de.catma.repository.db.maintenance;

public interface SourceDocumentIndexMaintainer {
	public int checkSourceDocumentIndex(int maxObjectCount, int offset) throws Exception;
}

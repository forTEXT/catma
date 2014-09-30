package de.catma.repository.db.maintenance;

import javax.naming.Context;


public interface SourceDocumentIndexMaintainer {
	public int checkSourceDocumentIndex(Context context, int maxObjectCount, int offset) throws Exception;
}

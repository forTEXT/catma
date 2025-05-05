package de.catma.api.v1.serialization.models;

import java.util.List;

import de.catma.util.IDGenerator;

@Deprecated
public class LegacyExport {
    private final String exportId;
    private final List<LegacyExportDocument> documents;

    public LegacyExport(List<LegacyExportDocument> exportDocuments) {
		exportId = new IDGenerator().generateExportId();
		this.documents = exportDocuments;
	}

    public String getExportId() {
        return exportId;
    }

    public List<LegacyExportDocument> getDocuments() {
        return documents;
    }
}

package de.catma.api.pre.serialization.models;

import java.util.List;

import de.catma.util.IDGenerator;

@Deprecated
public class LegacyExport {
    private final String exportId;
    private final List<LegacyExportDocument> exportDocuments;

    public LegacyExport(List<LegacyExportDocument> exportDocuments) {
		exportId = new IDGenerator().generateExportId();
		this.exportDocuments = exportDocuments;
	}

    public String getExportId() {
        return exportId;
    }

    public List<LegacyExportDocument> getExportDocuments() {
        return exportDocuments;
    }
}
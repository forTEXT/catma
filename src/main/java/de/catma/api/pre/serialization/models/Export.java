package de.catma.api.pre.serialization.models;

import java.util.List;

import de.catma.util.IDGenerator;

public class Export {
    private final String exportId;
    private final List<ExportDocument> exportDocuments;

    public Export(List<ExportDocument> exportDocuments) {
		exportId = new IDGenerator().generateExportId();
		this.exportDocuments = exportDocuments;
	}

    public String getExportId() {
        return exportId;
    }

    public List<ExportDocument> getExportDocuments() {
        return exportDocuments;
    }
}

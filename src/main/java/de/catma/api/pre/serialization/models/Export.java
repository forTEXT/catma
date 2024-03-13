package de.catma.api.pre.serialization.models;

import de.catma.util.IDGenerator;

import java.util.ArrayList;
import java.util.List;

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

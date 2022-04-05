package de.catma.api.pre.serialization.models;

import de.catma.util.IDGenerator;

import java.util.ArrayList;
import java.util.List;

public class Export {
    private String exportId;
    private List<ExportDocument> exportDocuments;

    public Export() {
        exportId = new IDGenerator().generateExportId();
        exportDocuments = new ArrayList<>();
    }

    public String getExportId() {
        return exportId;
    }

    public void setExportId(String exportId) {
        this.exportId = exportId;
    }

    public List<ExportDocument> getExportDocuments() {
        return exportDocuments;
    }

    public void setExportDocuments(List<ExportDocument> exportDocuments) {
        this.exportDocuments = exportDocuments;
    }

    public void addExportDocument(ExportDocument exportDocument) {
        exportDocuments.add(exportDocument);
    }
}

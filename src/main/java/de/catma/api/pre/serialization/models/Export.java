package de.catma.api.pre.serialization.models;

import java.util.List;

import de.catma.util.IDGenerator;

public class Export {
    private final String exportId;
    private final ExtendedMetadata extendedMetadata;
    private final List<ExportDocument> documents;

    public Export(ExtendedMetadata extendedMetadata, List<ExportDocument> documents) {
		exportId = new IDGenerator().generateExportId();
		this.documents = documents;
		this.extendedMetadata = extendedMetadata;
	}

    public String getExportId() {
        return exportId;
    }

    public ExtendedMetadata getExtendedMetadata() {
    	return extendedMetadata;
    }

    public List<ExportDocument> getDocuments() {
        return documents;
    }
}

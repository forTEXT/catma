package de.catma.api.pre.serialization.models;

import java.time.ZonedDateTime;
import java.util.List;

import de.catma.util.IDGenerator;

public class Export {
    private final String exportId;
    private final ZonedDateTime exportCreatedAt;
    private final int totalPages;
    private final int pageNo;
    private final int pageSize;
    private final String prevPage;
    private final String nextPage;
    private final ExtendedMetadata extendedMetadata;
    private final List<ExportDocument> documents;

    public Export(int totalPages, int pageNo, int pageSize, String prevPage, String nextPage,
                  ExtendedMetadata extendedMetadata, List<ExportDocument> documents) {
        exportId = new IDGenerator().generateExportId();
        exportCreatedAt = ZonedDateTime.now();

        this.totalPages = totalPages;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.prevPage = prevPage;
        this.nextPage = nextPage;
        this.extendedMetadata = extendedMetadata;
        this.documents = documents;
    }

    public String getExportId() {
        return exportId;
    }

    public ZonedDateTime getExportCreatedAt() {
        return exportCreatedAt;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getPageNo() {
        return pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public String getPrevPage() {
        return prevPage;
    }

    public String getNextPage() {
        return nextPage;
    }

    public ExtendedMetadata getExtendedMetadata() {
    	return extendedMetadata;
    }

    public List<ExportDocument> getDocuments() {
        return documents;
    }
}

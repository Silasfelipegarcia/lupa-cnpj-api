package br.com.lupainsights.dto;

public class AdminUsageSnapshotResponse {

    private long batchSearches;
    private long directCnpjLookups;

    public AdminUsageSnapshotResponse() {
    }

    public AdminUsageSnapshotResponse(long batchSearches, long directCnpjLookups) {
        this.batchSearches = batchSearches;
        this.directCnpjLookups = directCnpjLookups;
    }

    public long getBatchSearches() {
        return batchSearches;
    }

    public void setBatchSearches(long batchSearches) {
        this.batchSearches = batchSearches;
    }

    public long getDirectCnpjLookups() {
        return directCnpjLookups;
    }

    public void setDirectCnpjLookups(long directCnpjLookups) {
        this.directCnpjLookups = directCnpjLookups;
    }

    public long getTotal() {
        return batchSearches + directCnpjLookups;
    }
}

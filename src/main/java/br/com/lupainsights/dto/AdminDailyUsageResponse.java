package br.com.lupainsights.dto;

import java.time.LocalDate;

public class AdminDailyUsageResponse {

    private LocalDate date;
    private int batchSearches;
    private int directCnpjLookups;

    public AdminDailyUsageResponse() {
    }

    public AdminDailyUsageResponse(LocalDate date, int batchSearches, int directCnpjLookups) {
        this.date = date;
        this.batchSearches = batchSearches;
        this.directCnpjLookups = directCnpjLookups;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getBatchSearches() {
        return batchSearches;
    }

    public void setBatchSearches(int batchSearches) {
        this.batchSearches = batchSearches;
    }

    public int getDirectCnpjLookups() {
        return directCnpjLookups;
    }

    public void setDirectCnpjLookups(int directCnpjLookups) {
        this.directCnpjLookups = directCnpjLookups;
    }
}

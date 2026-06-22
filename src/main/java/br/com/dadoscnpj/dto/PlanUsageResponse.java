package br.com.dadoscnpj.dto;

public class PlanUsageResponse {

    private int maxRowsPerFile;
    private Integer maxBatchSearchesPerDay;
    private Integer maxDirectCnpjPerDay;
    private int batchSearchesToday;
    private int directCnpjToday;
    private boolean master;

    public int getMaxRowsPerFile() {
        return maxRowsPerFile;
    }

    public void setMaxRowsPerFile(int maxRowsPerFile) {
        this.maxRowsPerFile = maxRowsPerFile;
    }

    public Integer getMaxBatchSearchesPerDay() {
        return maxBatchSearchesPerDay;
    }

    public void setMaxBatchSearchesPerDay(Integer maxBatchSearchesPerDay) {
        this.maxBatchSearchesPerDay = maxBatchSearchesPerDay;
    }

    public Integer getMaxDirectCnpjPerDay() {
        return maxDirectCnpjPerDay;
    }

    public void setMaxDirectCnpjPerDay(Integer maxDirectCnpjPerDay) {
        this.maxDirectCnpjPerDay = maxDirectCnpjPerDay;
    }

    public int getBatchSearchesToday() {
        return batchSearchesToday;
    }

    public void setBatchSearchesToday(int batchSearchesToday) {
        this.batchSearchesToday = batchSearchesToday;
    }

    public int getDirectCnpjToday() {
        return directCnpjToday;
    }

    public void setDirectCnpjToday(int directCnpjToday) {
        this.directCnpjToday = directCnpjToday;
    }

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
    }
}

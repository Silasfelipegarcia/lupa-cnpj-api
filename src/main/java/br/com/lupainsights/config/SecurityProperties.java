package br.com.lupainsights.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private int importPerHour = 5;
    private int statusPerMinute = 30;
    private int downloadPerHour = 10;
    private int templatePerHour = 10;
    private int authPerMinute = 10;
    private int maxRowsPerFile = 200;
    private int maxQueueSize = 10;
    private int maxJobsInMemory = 50;
    private int maxActiveJobsPerIp = 1;
    private int maxActiveJobsPerUser = 1;
    private int jobTtlHours = 24;
    private int guestPreviewMax = 3;
    private long guestPreviewWindowDays = 365;
    private int guestPreviewPerMinute = 10;

    public int getImportPerHour() {
        return importPerHour;
    }

    public void setImportPerHour(int importPerHour) {
        this.importPerHour = importPerHour;
    }

    public int getStatusPerMinute() {
        return statusPerMinute;
    }

    public void setStatusPerMinute(int statusPerMinute) {
        this.statusPerMinute = statusPerMinute;
    }

    public int getDownloadPerHour() {
        return downloadPerHour;
    }

    public void setDownloadPerHour(int downloadPerHour) {
        this.downloadPerHour = downloadPerHour;
    }

    public int getTemplatePerHour() {
        return templatePerHour;
    }

    public void setTemplatePerHour(int templatePerHour) {
        this.templatePerHour = templatePerHour;
    }

    public int getAuthPerMinute() {
        return authPerMinute;
    }

    public void setAuthPerMinute(int authPerMinute) {
        this.authPerMinute = authPerMinute;
    }

    public int getMaxRowsPerFile() {
        return maxRowsPerFile;
    }

    public void setMaxRowsPerFile(int maxRowsPerFile) {
        this.maxRowsPerFile = maxRowsPerFile;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public int getMaxJobsInMemory() {
        return maxJobsInMemory;
    }

    public void setMaxJobsInMemory(int maxJobsInMemory) {
        this.maxJobsInMemory = maxJobsInMemory;
    }

    public int getMaxActiveJobsPerIp() {
        return maxActiveJobsPerIp;
    }

    public void setMaxActiveJobsPerIp(int maxActiveJobsPerIp) {
        this.maxActiveJobsPerIp = maxActiveJobsPerIp;
    }

    public int getMaxActiveJobsPerUser() {
        return maxActiveJobsPerUser;
    }

    public void setMaxActiveJobsPerUser(int maxActiveJobsPerUser) {
        this.maxActiveJobsPerUser = maxActiveJobsPerUser;
    }

    public int getJobTtlHours() {
        return jobTtlHours;
    }

    public void setJobTtlHours(int jobTtlHours) {
        this.jobTtlHours = jobTtlHours;
    }

    public int getGuestPreviewMax() {
        return guestPreviewMax;
    }

    public void setGuestPreviewMax(int guestPreviewMax) {
        this.guestPreviewMax = guestPreviewMax;
    }

    public long getGuestPreviewWindowDays() {
        return guestPreviewWindowDays;
    }

    public void setGuestPreviewWindowDays(long guestPreviewWindowDays) {
        this.guestPreviewWindowDays = guestPreviewWindowDays;
    }

    public int getGuestPreviewPerMinute() {
        return guestPreviewPerMinute;
    }

    public void setGuestPreviewPerMinute(int guestPreviewPerMinute) {
        this.guestPreviewPerMinute = guestPreviewPerMinute;
    }
}

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
    private int paymentsPerMinute = 20;
    private int analyticsPerMinute = 60;
    private int passwordChangePerHour = 5;
    private int passwordResetForgotPerHour = 3;
    private int passwordResetPerMinute = 10;
    private int importPerUserPerHour = 10;
    private int loginFailuresBeforeLock = 5;
    private int loginLockMinutes = 15;
    private boolean trustProxy = false;
    private int historicoPerMinute = 30;
    private int readPerMinute = 60;
    private int consultaPerMinute = 30;
    private int adminPerMinute = 30;
    private int adminBootstrapPerHour = 3;
    private String redisUrl = "";

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

    public int getPaymentsPerMinute() {
        return paymentsPerMinute;
    }

    public void setPaymentsPerMinute(int paymentsPerMinute) {
        this.paymentsPerMinute = paymentsPerMinute;
    }

    public int getAnalyticsPerMinute() {
        return analyticsPerMinute;
    }

    public void setAnalyticsPerMinute(int analyticsPerMinute) {
        this.analyticsPerMinute = analyticsPerMinute;
    }

    public int getPasswordChangePerHour() {
        return passwordChangePerHour;
    }

    public void setPasswordChangePerHour(int passwordChangePerHour) {
        this.passwordChangePerHour = passwordChangePerHour;
    }

    public int getPasswordResetForgotPerHour() {
        return passwordResetForgotPerHour;
    }

    public void setPasswordResetForgotPerHour(int passwordResetForgotPerHour) {
        this.passwordResetForgotPerHour = passwordResetForgotPerHour;
    }

    public int getPasswordResetPerMinute() {
        return passwordResetPerMinute;
    }

    public void setPasswordResetPerMinute(int passwordResetPerMinute) {
        this.passwordResetPerMinute = passwordResetPerMinute;
    }

    public int getImportPerUserPerHour() {
        return importPerUserPerHour;
    }

    public void setImportPerUserPerHour(int importPerUserPerHour) {
        this.importPerUserPerHour = importPerUserPerHour;
    }

    public int getLoginFailuresBeforeLock() {
        return loginFailuresBeforeLock;
    }

    public void setLoginFailuresBeforeLock(int loginFailuresBeforeLock) {
        this.loginFailuresBeforeLock = loginFailuresBeforeLock;
    }

    public int getLoginLockMinutes() {
        return loginLockMinutes;
    }

    public void setLoginLockMinutes(int loginLockMinutes) {
        this.loginLockMinutes = loginLockMinutes;
    }

    public boolean isTrustProxy() {
        return trustProxy;
    }

    public void setTrustProxy(boolean trustProxy) {
        this.trustProxy = trustProxy;
    }

    public int getHistoricoPerMinute() {
        return historicoPerMinute;
    }

    public void setHistoricoPerMinute(int historicoPerMinute) {
        this.historicoPerMinute = historicoPerMinute;
    }

    public int getReadPerMinute() {
        return readPerMinute;
    }

    public void setReadPerMinute(int readPerMinute) {
        this.readPerMinute = readPerMinute;
    }

    public int getConsultaPerMinute() {
        return consultaPerMinute;
    }

    public void setConsultaPerMinute(int consultaPerMinute) {
        this.consultaPerMinute = consultaPerMinute;
    }

    public int getAdminPerMinute() {
        return adminPerMinute;
    }

    public void setAdminPerMinute(int adminPerMinute) {
        this.adminPerMinute = adminPerMinute;
    }

    public int getAdminBootstrapPerHour() {
        return adminBootstrapPerHour;
    }

    public void setAdminBootstrapPerHour(int adminBootstrapPerHour) {
        this.adminBootstrapPerHour = adminBootstrapPerHour;
    }

    public String getRedisUrl() {
        return redisUrl;
    }

    public void setRedisUrl(String redisUrl) {
        this.redisUrl = redisUrl;
    }

    public boolean isRedisEnabled() {
        return redisUrl != null && !redisUrl.isBlank();
    }
}

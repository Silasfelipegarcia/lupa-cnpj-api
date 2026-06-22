package br.com.dadoscnpj.dto;

public class AnalyticsEventRequest {

    private String event;
    private String properties;

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }
}

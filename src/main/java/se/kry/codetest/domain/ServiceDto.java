package se.kry.codetest.domain;

import se.kry.codetest.domain.enumeration.ServiceStatus;

public class ServiceDto {

    private final String url;
    private final String name;
    private final ServiceStatus status;

    public ServiceDto(String url, String name, ServiceStatus status) {
        this.url = url;
        this.name = name;
        this.status = status;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public ServiceStatus getStatus() {
        return status;
    }
}

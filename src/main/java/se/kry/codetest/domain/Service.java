package se.kry.codetest.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Objects;

public class Service {

    private final String url;
    private final String name;
    private final Instant creationDate;


    public Service(final String url, final String name, Instant creationDate) {
        this.url = url;
        this.name = name;
        this.creationDate = creationDate;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Service service = (Service) o;
        return Objects.equals(url, service.url) &&
                Objects.equals(name, service.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, name);
    }
}

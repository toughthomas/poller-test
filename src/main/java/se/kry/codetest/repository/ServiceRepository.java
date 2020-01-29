package se.kry.codetest.repository;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import se.kry.codetest.DBConnector;
import se.kry.codetest.domain.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ServiceRepository {

    private final DBConnector connector;

    public ServiceRepository(DBConnector connector) {
        this.connector = connector;
    }

    public void deleteByName(final String serviceName) {
        connector.query("delete from service where name = ?", new JsonArray().add(serviceName));
    }

    public void save(final Service service) {
        connector.query("insert into service values (?, ?, ?)", new JsonArray()
                .add(service.getUrl())
                .add(service.getName())
                .add(service.getCreationDate().toEpochMilli()));
    }

    public Future<List<Service>> getAll() {
        final List<Service> results = new ArrayList<>();
        return connector.query("select * from service").map( (ResultSet resultSet ) -> {
            for (JsonArray line : resultSet.getResults()) {
                results.add(new Service(line.getString(0), line.getString(1), Instant.ofEpochMilli(line.getLong(2))));
            }
            return results;
        });

    }
}

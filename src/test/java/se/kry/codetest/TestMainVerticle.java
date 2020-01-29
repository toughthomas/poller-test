package se.kry.codetest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava.ext.unit.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import se.kry.codetest.domain.InsertServiceCmd;
import se.kry.codetest.domain.Service;
import se.kry.codetest.domain.enumeration.ServiceStatus;
import se.kry.codetest.repository.ServiceRepository;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

    private static final Service TEST_SERVICE = new Service("http://www.test.fr", "test", Instant.now());
    private static final InsertServiceCmd INSERT_SERVICE_CMD = new InsertServiceCmd(TEST_SERVICE.getUrl(), TEST_SERVICE.getName());
    private static final InsertServiceCmd MALFORMED_URL_INSERT_SERVICE_CMD = new InsertServiceCmd("rred", TEST_SERVICE.getName());
    private ServiceRepository repository;
    private DBConnector dbConnector;

    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        dbConnector = new DBConnector(vertx);
        repository = new ServiceRepository(dbConnector);
        vertx.deployVerticle(new ServiceVerticle(repository, new BackgroundPoller(vertx)));
        vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
        dbConnector.query("delete from service");
    }


    @Test
    @DisplayName("Get endpoint should retrieve saved services")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void should_retrieve_posted_services(Vertx vertx, VertxTestContext testContext) {
        vertx.eventBus().publish(EventTopic.POST_SERVICE, new JsonObject(Json.encode(new InsertServiceCmd(TEST_SERVICE.getUrl(), TEST_SERVICE.getName()))));
        WebClient.create(vertx)
                .get(8080, "::1", "/service")
                .send(response -> testContext.verify(() -> {
                    assertEquals(200, response.result().statusCode());
                    JsonArray body = response.result().bodyAsJsonArray();
                    assertEquals(1, body.size());
                    assertEquals(TEST_SERVICE.getName(), body.getJsonObject(0).getString("name"));
                    assertEquals(TEST_SERVICE.getUrl(), body.getJsonObject(0).getString("url"));
                    assertEquals(ServiceStatus.UNKNOWN.toString(), body.getJsonObject(0).getString("status"));
                    testContext.completeNow();
                }));

    }

    @Test
    @Timeout(value = 5, timeUnit = TimeUnit.SECONDS)
    void posted_service_should_be_saved_to_database(Vertx vertx, VertxTestContext testContext) {
        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJson(INSERT_SERVICE_CMD, response -> testContext.verify(() -> {
                    repository.getAll().setHandler(result -> {
                        List<Service> databaseServices = result.result();
                        assertEquals(1, databaseServices.size());
                        assertEquals(TEST_SERVICE.getName(), databaseServices.get(0).getName());
                        assertEquals(TEST_SERVICE.getUrl(), databaseServices.get(0).getUrl());
                        testContext.completeNow();
                    });

                }));
    }

    @Test
    @Timeout(value = 5, timeUnit = TimeUnit.SECONDS)
    void post_service_with_malformed_url_should_return_badrequest(Vertx vertx, VertxTestContext testContext) {
        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJson(MALFORMED_URL_INSERT_SERVICE_CMD, response -> testContext.verify(() -> {
                    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), response.result().statusCode());
                    testContext.completeNow();
                }));
    }

    @Test
    @Timeout(value = 5, timeUnit = TimeUnit.SECONDS)
    void delete_should_delete_service_from_database(Vertx vertx, VertxTestContext testContext) {
        repository.save(TEST_SERVICE);
        WebClient.create(vertx)
                .delete(8080, "::1", "/service/" + TEST_SERVICE.getName())
                .send(response -> testContext.verify(() -> {
                    try {
                        Thread.sleep(2000); // Could be more elegant
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    repository.getAll().setHandler(result -> {
                        List<Service> databaseServices = result.result();
                        assertEquals(0, databaseServices.size());
                        testContext.completeNow();
                    });
                }));
    }

}

package se.kry.codetest;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import se.kry.codetest.domain.Service;
import se.kry.codetest.domain.enumeration.ServiceStatus;

import java.util.function.Consumer;

public class BackgroundPoller {

    private final WebClient client;

    public BackgroundPoller(final Vertx vertx) {
        client = WebClient.create(vertx, new WebClientOptions().setConnectTimeout(3000));
    }

    public void pollService(final Service service, final Consumer<ServiceStatus> statusConsumer) {
        client.getAbs(service.getUrl())
                .send(response -> {
                    if (response.succeeded()) {
                        statusConsumer.accept(ServiceStatus.UP);
                    } else {
                        statusConsumer.accept(ServiceStatus.DOWN);
                    }
                });
    }
}

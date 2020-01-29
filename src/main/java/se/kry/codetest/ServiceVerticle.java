package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import se.kry.codetest.domain.Service;
import se.kry.codetest.domain.ServiceDto;
import se.kry.codetest.domain.enumeration.ServiceStatus;
import se.kry.codetest.repository.ServiceRepository;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ServiceVerticle extends AbstractVerticle {

    private ServiceRepository serviceRepository;
    private BackgroundPoller poller;
    private final Map<Service, ServiceStatus> serviceServiceStatusMap = new ConcurrentHashMap<>();

    public ServiceVerticle(ServiceRepository serviceRepository, BackgroundPoller poller) {
        this.serviceRepository = serviceRepository;
        this.poller = poller;
    }

    @Override
    public void start(Future<Void> startFuture) {
        serviceRepository.getAll().setHandler(databaseServices -> databaseServices.result().forEach(this::addNewServiceToMap));
        vertx.setPeriodic(1000 * 60, timerId -> {
            serviceServiceStatusMap.keySet().forEach(service -> {
                poller.pollService(service, serviceStatus -> serviceServiceStatusMap.replace(service, serviceStatus));
            });

        });
        vertx.eventBus().consumer(EventTopic.GET_SERVICE, getMessageHandler());
        vertx.eventBus().consumer(EventTopic.POST_SERVICE, postMessageHandler());
        vertx.eventBus().consumer(EventTopic.DELETE_SERVICE, deleteMessageHandler());
        startFuture.complete();
    }

    private Handler<Message<String>> deleteMessageHandler() {
        return serviceName -> {
            serviceServiceStatusMap.keySet().stream()
                    .filter(service -> service.getName().equals(serviceName.body()))
                    .forEach(serviceServiceStatusMap::remove);
            serviceRepository.deleteByName(serviceName.body());
        };
    }

    private Handler<Message<JsonObject>> postMessageHandler() {
        return message -> {
            Service service = new Service(message.body().getString("url"), message.body().getString("name"), Instant.now());
            addNewServiceToMap(service);
            serviceRepository.save(service);
            message.reply(Json.encode(service));
        };
    }

    private void addNewServiceToMap(Service service) {
        serviceServiceStatusMap.put(service, ServiceStatus.UNKNOWN);
    }

    private Handler<Message<Object>> getMessageHandler() {
        return message -> message.reply(new JsonArray(serviceServiceStatusMap.entrySet().stream().map(
                entry -> new ServiceDto(entry.getKey().getUrl(), entry.getKey().getName(), entry.getValue())
        ).collect(Collectors.toList())).encode());
    }

}

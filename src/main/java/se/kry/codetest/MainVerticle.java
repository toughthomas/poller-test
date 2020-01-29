package se.kry.codetest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import se.kry.codetest.domain.InsertServiceCmd;
import se.kry.codetest.domain.Service;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    @Override
    public void start(Future<Void> startFuture) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        setRoutes(router);
        vertx
                .createHttpServer()
                .requestHandler(router)
                .listen(8080, result -> {
                    if (result.succeeded()) {
                        System.out.println("KRY code test service started");
                        startFuture.complete();
                    } else {
                        startFuture.fail(result.cause());
                    }
                });
    }

    private void setRoutes(Router router) {
        router.route("/*").handler(StaticHandler.create());
        router.get("/service").handler(req -> {
            vertx.eventBus().<String>send(EventTopic.GET_SERVICE, new JsonArray(), message -> {
                req.response()
                        .putHeader("content-type", "application/json")
                        .end(message.result().body());
            });
        });

        router.delete("/service/:name").handler(req -> {
            vertx.eventBus().publish(EventTopic.DELETE_SERVICE, req.request().getParam("name"));
            req.response().setStatusCode(HttpResponseStatus.ACCEPTED.code()).end();
        });

        router.post("/service")
                .handler(req -> {
                    InsertServiceCmd insertServiceCmd = req.getBodyAsJson().mapTo(InsertServiceCmd.class);
                    if (!validator.validate(insertServiceCmd).isEmpty()) {
                        req.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
                    } else {
                        req.next();
                    }
                })
                .handler(req -> {
                    vertx.eventBus().send(EventTopic.POST_SERVICE, req.getBodyAsJson(), message -> {
                        if (message.succeeded()) {
                            req.response()
                                    .putHeader("content-type", "application/json")
                                    .end((String) message.result().body());
                        } else {
                            req.response()
                                    .putHeader("content-type", "text/plain")
                                    .end("ERROR");
                        }
                    });
                })
                .failureHandler(ctx -> System.err.println(ctx.failure().getMessage()));
    }

}




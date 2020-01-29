package se.kry.codetest;

import io.vertx.core.Vertx;
import se.kry.codetest.repository.ServiceRepository;

public class Start {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new ServiceVerticle(new ServiceRepository(new DBConnector(vertx)), new BackgroundPoller(vertx)));
    vertx.deployVerticle(new MainVerticle());
  }


}

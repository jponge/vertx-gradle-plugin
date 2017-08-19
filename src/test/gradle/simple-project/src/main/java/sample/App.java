package sample;

import io.vertx.core.Vertx;
import io.vertx.core.AbstractVerticle;

public class App extends AbstractVerticle {

  @Override
  public void start() {
    vertx
      .createHttpServer()
      .requestHandler(req -> req.response().end("Yo!")).listen(8080);
  }
}

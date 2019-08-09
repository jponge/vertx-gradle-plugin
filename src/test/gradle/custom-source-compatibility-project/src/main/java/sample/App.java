package sample;

import io.vertx.core.Vertx;
import io.vertx.core.AbstractVerticle;

public class App extends AbstractVerticle {

  @Override
  public void start() {

    vertx
      .createHttpServer()
      .requestHandler(req -> req.response().end("Yo!")).listen(18080);

    vertx
      .createHttpServer()
      .requestHandler(req -> {
        req.response().end("Bye!");
        vertx.close();
      }).listen(18081);
  }
}

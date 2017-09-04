package sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class App extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(App.class);

  @Override
  public void start() {
    Router router = Router.router(vertx);

    router.get("/").handler(ctx -> {
      ctx.response().putHeader("Content-Type", "text/plain").end("This is the root resource");
    });

    router.get("/plop").handler(ctx -> {
      ctx.response()
        .putHeader("Content-Type", "application/json")
        .end(new JsonObject().put("what", "Plop").encodePrettily());
    });

    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(8080);
  }

  public static void main(String[] args) {
    logger.info("Starting...");
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new App(), id -> {
      if (id.succeeded()) {
        logger.info("App verticle successfully deployed");
      } else {
        logger.error("Deployment of App verticle failed", id.cause());
      }
    });
  }
}

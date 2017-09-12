package sample;

import java.util.Date;

import io.vertx.core.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start() {
    Router router = Router.router(vertx);

    router.get("/static/*").handler(StaticHandler.create().setCachingEnabled(false));

    router.get("/").handler(req -> req.reroute("/static/index.html"));

    router.get("/time").handler(req -> req.response()
      .putHeader("Content-Type", "application/json")
      .end(new JsonObject()
        .put("what", "Time at some point")
        .put("value", new Date().toString()).encode()));

    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(8080);
  }
}

package sample

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import io.vertx.core.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router

class App : AbstractVerticle() {

  private val logger = LoggerFactory.getLogger(App::class.java);

  override fun start() {
    val router = Router.router(vertx)

    router.get("/").handler() { ctx ->
      ctx.response().putHeader("Content-Type", "text/plain").end("This is the root resource")
    }

    router.get("/plop").handler() { ctx ->
      ctx.response()
        .putHeader("Content-Type", "application/json")
        .end(JsonObject().put("what", "Plop").encodePrettily())
    }

    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(8080)
  }
}

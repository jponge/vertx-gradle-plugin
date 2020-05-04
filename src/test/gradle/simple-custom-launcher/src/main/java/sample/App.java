package sample;

import io.vertx.core.Vertx;
import io.vertx.core.AbstractVerticle;

public class App extends AbstractVerticle {

  @Override
  public void start() {
    vertx.close(foo -> { System.out.println("App stopped."); });
  }
}

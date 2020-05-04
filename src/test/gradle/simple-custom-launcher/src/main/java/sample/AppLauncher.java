package sample;

import io.vertx.core.Launcher;
import io.vertx.core.Vertx;

public class AppLauncher extends Launcher {

  @Override
  public void afterStartingVertx(Vertx vertx) {
    System.out.println("Started with custom launcher.");
  }

  public static void main(String[] args) {
    new AppLauncher().dispatch(args);
  }
}

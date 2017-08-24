package io.vertx.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

/**
 * @author [Julien Ponge](https://julien.ponge.org/)
 */
class VertxPluginTest {

  @Test
  fun `smoke test`() {
    GradleRunner.create()
      .withProjectDir(File("src/test/gradle/simple-project"))
      .withPluginClasspath()
      .withArguments("tasks")
      .build()
  }
}

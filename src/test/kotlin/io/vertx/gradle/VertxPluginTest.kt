package io.vertx.gradle

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import com.github.kittinunf.fuel.*

/**
 * @author [Julien Ponge](https://julien.ponge.org/)
 */
class VertxPluginTest {

  @Test
  fun `smoke test`() {
    val runner = GradleRunner.create()
      .withProjectDir(File("src/test/gradle/simple-project"))
      .withPluginClasspath()
      .withArguments("tasks")
      .build()

    assertThat(runner.output).contains("shadowJar", "BUILD SUCCESSFUL")
  }

  @Test
  fun `check that it builds a fat jar`() {
    GradleRunner.create()
      .withProjectDir(File("src/test/gradle/simple-project"))
      .withPluginClasspath()
      .withArguments("clean", "build")
      .build()

    val fatJarFile = File("src/test/gradle/simple-project/build/libs/simple-project-fat.jar")
    assertThat(fatJarFile).exists().isFile()
    JarFile(fatJarFile).use {
      assertThat(it.getJarEntry("sample/App.class")).isNotNull()
      assertThat(it.getJarEntry("io/vertx/core/Vertx.class")).isNotNull()
      assertThat(it.getJarEntry("io/netty/channel/EventLoopGroup.class")).isNotNull()
    }
  }

  @Test
  fun `check that the application does run`() {
    GradleRunner.create()
      .withProjectDir(File("src/test/gradle/simple-project"))
      .withPluginClasspath()
      .withArguments("clean", "build")
      .build()

    run("java", "-jar", "src/test/gradle/simple-project/build/libs/simple-project-fat.jar") {
      val (req, resp, res) = "http://localhost:18080/".httpGet().responseString()
      assertThat(res.get()).isEqualTo("Yo!")
    }
  }
}

private fun run(vararg command: String, block: () -> Unit) {
  val process = ProcessBuilder(command.toList()).start()
  Thread.sleep(1000)
  block()
  process.destroyForcibly()
  process.waitFor(30, TimeUnit.SECONDS)
}

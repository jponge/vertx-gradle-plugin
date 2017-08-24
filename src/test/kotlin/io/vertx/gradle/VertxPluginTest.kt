package io.vertx.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File
import org.assertj.core.api.Assertions.*
import java.util.jar.JarFile

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
}

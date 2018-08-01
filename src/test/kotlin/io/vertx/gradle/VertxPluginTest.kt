/*
 * Copyright 2017-2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.vertx.gradle

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import com.mashape.unirest.http.Unirest

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
      val response = Unirest.get("http://localhost:18080/").asString()
      Unirest.shutdown()
      assertThat(response.status).isEqualTo(200)
      assertThat(response.body).isEqualTo("Yo!")
    }
  }

  @Test
  fun `check that it builds a Docker image with Jib`() {
    GradleRunner.create()
      .withProjectDir(File("src/test/gradle/simple-project"))
      .withPluginClasspath()
      .withArguments("clean", "jibDockerBuild")
      .build()
    run("docker", "pull", "simple-project")
    val inspection = run("docker", "inspect", "simple-project")
    assertThat(inspection).containsSequence("simple-project:latest")
    assertThat(inspection).containsSequence("\"io.vertx.core.Launcher\"")
  }
}

private fun run(vararg command: String, block: () -> Unit = {}): String {
  val process = ProcessBuilder(command.toList())
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .start()
  Thread.sleep(1000)
  block()
  val stdout = process.inputStream.bufferedReader().readText()
  process.destroyForcibly()
  process.waitFor(30, TimeUnit.SECONDS)
  return stdout
}

/*
 * Copyright 2017-2019 Red Hat, Inc.
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

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.*
import org.gradle.api.tasks.JavaExec
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

/**
 * A Gradle plugin for Eclipse Vert.x projects.
 *
 * @author [Julien Ponge](https://julien.ponge.org/)
 */
class VertxPlugin : Plugin<Project> {

  private val logger = LoggerFactory.getLogger(VertxPlugin::class.java)

  private lateinit var gradleCommand: String

  override fun apply(project: Project) {
    findGradleCommand(project)
    installVertxExtension(project)
    applyOtherPlugins(project)
    createVertxTasks(project)
    project.afterEvaluate {
      logger.debug("Vert.x plugin configuration: ${project.vertxExtension()}")
      configureVertxVersion(project)
      addVertxCoreDependency(project)
      defineMainClassName(project)
      configureShadowPlugin(project)
      configureVertxRunTask(project)
      configureVertxDebugTask(project)
    }
  }

  private fun findGradleCommand(project: Project) {
    val globalGradle = if (Os.isFamily(Os.FAMILY_WINDOWS)) "gradle.bat" else "gradle"
    val gradlewScript = if (Os.isFamily(Os.FAMILY_WINDOWS)) "gradlew.bat" else "gradlew"

    fun findRecursively(dir: File): Optional<String> {
      val script = File(dir, gradlewScript)
      return when {
        script.exists() -> Optional.of(script.absolutePath)
        dir.parentFile != null -> findRecursively(dir.parentFile)
        else -> Optional.empty()
      }
    }

    gradleCommand = findRecursively(project.projectDir).orElse(globalGradle)
  }

  private fun installVertxExtension(project: Project) {
    project.extensions.create("vertx", VertxExtension::class.java, project)
    logger.debug("vertx extension created and added to the project")
  }

  private fun Project.vertxExtension(): VertxExtension = this.extensions.getByName("vertx") as VertxExtension

  private fun applyOtherPlugins(project: Project) {
    logger.debug("Applying the plugins needed by the Vert.x plugin")
    with(project.pluginManager) {
      apply(JavaPlugin::class.java)
      apply(ApplicationPlugin::class.java)
      apply(ShadowPlugin::class.java)
    }
    logger.debug("The plugins needed by the Vert.x plugin have been applied")
  }

  private fun configureVertxVersion(project: Project) {
    val vertxVersion = project.vertxExtension().vertxVersion
    project.extensions.extraProperties["stack.version"] = vertxVersion
    project.dependencies.apply {
      add("implementation", platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
    }
    logger.debug("Recommending Vert.x version $vertxVersion")
  }

  private fun addVertxCoreDependency(project: Project) {
    project.dependencies.apply {
      add("implementation", "io.vertx:vertx-core")
    }
    logger.debug("Added vertx-core as a compile dependency")
  }

  private fun defineMainClassName(project: Project) {
    val vertxExtension = project.vertxExtension()
    val applicationConvention = project.convention.getPlugin(ApplicationPluginConvention::class.java)
    applicationConvention.mainClassName = vertxExtension.launcher
    logger.debug("The main class has been set to ${vertxExtension.launcher}")
  }

  private fun configureShadowPlugin(project: Project) {
    val shadowJarTask = project.tasks.getByName("shadowJar") as ShadowJar
    val vertxExtension = project.vertxExtension()
    shadowJarTask.apply {
      archiveClassifier.set("all")
      mergeServiceFiles { serviceFiles ->
        serviceFiles.include("META-INF/services/io.vertx.core.spi.VerticleFactory")
        serviceFiles.include("META-INF/spring.*")
      }
      manifest { manifest ->
        manifest.attributes["Main-Verticle"] = vertxExtension.mainVerticle
      }
    }
    logger.debug("The shadow plugin has been configured")
  }

  private fun createVertxTasks(project: Project) {
    project.tasks.create("vertxRun", JavaExec::class.java).dependsOn("classes")
    project.tasks.create("vertxDebug", JavaExec::class.java).dependsOn("classes")
    logger.debug("The vertx tasks have been created")
  }

  private fun configureVertxRunTask(project: Project) {
    val vertxExtension = project.vertxExtension()
    val javaConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
    val mainSourceSet = javaConvention.sourceSets.getByName("main")

    (project.tasks.getByName("vertxRun") as JavaExec).apply {
      group = "Application"
      description = "Runs this project as a Vert.x application"

      workingDir(File(vertxExtension.workDirectory))
      jvmArgs(vertxExtension.jvmArgs)
      classpath(mainSourceSet.runtimeClasspath)

      main = if (vertxExtension.redeploy) "io.vertx.core.Launcher" else vertxExtension.launcher

      if (vertxExtension.mainVerticle.isBlank()) {
        if (vertxExtension.launcher == "io.vertx.core.Launcher") {
          throw GradleException("Extension property vertx.mainVerticle must be specified when using io.vertx.core.Launcher as a launcher")
        }
        args("run")
      } else {
        args("run", vertxExtension.mainVerticle)
      }

      if (vertxExtension.redeploy) {
        args("--launcher-class", vertxExtension.launcher)
        if (vertxExtension.jvmArgs.isNotEmpty()) {
          args("--java-opts", vertxExtension.jvmArgs.joinToString(separator = " ", prefix = "\"", postfix = "\""))
        }
        args("--redeploy", vertxExtension.watch.joinToString(separator = ","))
        if (vertxExtension.onRedeploy.isNotEmpty()) {
          args("--on-redeploy", "$gradleCommand ${vertxExtension.onRedeploy.joinToString(separator = " ")}")
        }
        args("--redeploy-grace-period", vertxExtension.redeployGracePeriod)
        args("--redeploy-scan-period", vertxExtension.redeployScanPeriod)
        args("--redeploy-termination-period", vertxExtension.redeployTerminationPeriod)
      }

      if (vertxExtension.config.isNotBlank()) {
        args("--conf", vertxExtension.config)
      }
      vertxExtension.args.forEach { args(it) }
    }

    logger.debug("The vertxRun task has been configured")
  }

  private fun configureVertxDebugTask(project: Project) {
    val vertxExtension = project.vertxExtension()
    val javaConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
    val mainSourceSet = javaConvention.sourceSets.getByName("main")

    (project.tasks.getByName("vertxDebug") as JavaExec).apply {
      group = "Application"
      description = "Debugs this project as a Vert.x application"

      workingDir(File(vertxExtension.workDirectory))
      jvmArgs(vertxExtension.jvmArgs)
      jvmArgs(computeDebugOptions(project))
      classpath(mainSourceSet.runtimeClasspath)

      main = vertxExtension.launcher

      if (vertxExtension.mainVerticle.isBlank()) {
        if (vertxExtension.launcher == "io.vertx.core.Launcher") {
          throw GradleException("Extension property vertx.mainVerticle must be specified when using io.vertx.core.Launcher as a launcher")
        }
        args("run")
      } else {
        args("run", vertxExtension.mainVerticle)
      }
      if (vertxExtension.config.isNotBlank()) {
        args("--conf", vertxExtension.config)
      }
      vertxExtension.args.forEach { args(it) }
    }

    logger.debug("The vertxDebug task has been configured")
  }

  private fun computeDebugOptions(project: Project): List<String> {
    val vertxExtension = project.vertxExtension()
    val debugger = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=" +
      (if (vertxExtension.debugSuspend) "y" else "n") + ",address=${vertxExtension.debugPort}"
    val disableEventLoopchecker = "-Dvertx.options.maxEventLoopExecuteTime=${java.lang.Long.MAX_VALUE}"
    val disableWorkerchecker = "-Dvertx.options.maxWorkerExecuteTime=${java.lang.Long.MAX_VALUE}"
    val mark = "-Dvertx.debug=true"

    return arrayListOf(debugger, disableEventLoopchecker, disableWorkerchecker, mark)
  }
}

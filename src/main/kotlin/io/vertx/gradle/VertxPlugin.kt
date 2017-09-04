/*
 * Copyright 2017 Red Hat, Inc.
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
import netflix.nebula.dependency.recommender.DependencyRecommendationsPlugin
import netflix.nebula.dependency.recommender.provider.RecommendationProviderContainer
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.ApplicationPluginConvention
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.JavaExec
import org.slf4j.LoggerFactory
import java.io.File

/**
 * A Gradle plugin for Eclipse Vert.x projects.
 *
 * @author [Julien Ponge](https://julien.ponge.org/)
 */
class VertxPlugin : Plugin<Project> {

  private val logger = LoggerFactory.getLogger(VertxPlugin::class.java)

  override fun apply(project: Project) {
    installVertxExtension(project)
    applyOtherPlugins(project)
    project.gradle.projectsEvaluated {
      logger.debug("Vert.x plugin configuration: ${project.vertxExtension()}")
      configureDependencyRecommendationslugin(project)
      addVertxCoreDependency(project)
      defineJavaSourceCompatibility(project)
      defineMainClassName(project)
      configureShadowPlugin(project)
      createVertxRunTask(project)
    }
  }

  private fun installVertxExtension(project: Project) {
    project.extensions.create("vertx", VertxExtension::class.java, project)
    logger.debug("vertx extension created and added to the project")
  }

  private fun Project.vertxExtension(): VertxExtension = this.extensions.getByName("vertx") as VertxExtension

  private fun applyOtherPlugins(project: Project) {
    logger.debug("Applying the plugins needed by the Vert.x plugin")
    project.pluginManager.apply(JavaPlugin::class.java)
    project.pluginManager.apply(ApplicationPlugin::class.java)
    project.pluginManager.apply(ShadowPlugin::class.java)
    project.pluginManager.apply(DependencyRecommendationsPlugin::class.java)
    logger.debug("The plugins needed by the Vert.x plugin have been applied")
  }

  private fun configureDependencyRecommendationslugin(project: Project) {
    val recommendations = project.extensions.getByName("dependencyRecommendations") as RecommendationProviderContainer
    val vertxVersion = project.vertxExtension().vertxVersion
    recommendations.apply {
      mavenBom(mapOf("module" to "io.vertx:vertx-dependencies:$vertxVersion"))
    }
    project.extensions.extraProperties["stack.version"] = vertxVersion
    logger.debug("Recommending Vert.x version $vertxVersion")
  }

  private fun addVertxCoreDependency(project: Project) {
    project.dependencies.apply {
      add("compile", "io.vertx:vertx-core")
    }
    logger.debug("Added vertx-core as a compile dependency")
  }

  private fun defineJavaSourceCompatibility(project: Project) {
    val javaConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
    val javaVersion = JavaVersion.VERSION_1_8
    javaConvention.sourceCompatibility = javaVersion
    javaConvention.targetCompatibility = javaVersion
    logger.debug("The Vert.x plugin has set Java compatibility to $javaVersion")
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
      classifier = "fat"
      mergeServiceFiles { serviceFiles ->
        serviceFiles.include("META-INF/services/io.vertx.core.spi.VerticleFactory")
      }
      manifest { manifest ->
        manifest.attributes.put("Main-Verticle", vertxExtension.mainVerticle)
      }
    }
    logger.debug("The shadow plugin has been configured")
  }

  private fun createVertxRunTask(project: Project) {
    val vertxExtension = project.vertxExtension()
    val javaConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
    val mainSourceSet = javaConvention.sourceSets.getByName("main")

    project.tasks.create("vertxRun", JavaExec::class.java).apply {
      group = "Application"
      description = "Runs this project as a Vert.x application"

      workingDir(File(vertxExtension.workDirectory))
      jvmArgs(vertxExtension.jvmArgs)
      classpath(mainSourceSet.runtimeClasspath)

      if (vertxExtension.redeploy) {
        main = "io.vertx.core.Launcher"

        if (vertxExtension.launcher == "io.vertx.core.Launcher") {
          if (vertxExtension.mainVerticle.isBlank()) {
            throw GradleException("Extension property vertx.mainVerticle must be specified when using io.vertx.core.Launcher as a launcher")
          }
          args("run", vertxExtension.mainVerticle)
        } else {
          args("run")
        }

        args("--launcher-class", vertxExtension.launcher)
        args("--redeploy")
        vertxExtension.watch.forEach { args(it) }
        args("--redeploy-grace-period", vertxExtension.redeployGracePeriod)
        args("--redeploy-scan-period", vertxExtension.redeployScanPeriod)
        args("--redeploy-termination-period", vertxExtension.redeployTerminationPeriod)
        if (vertxExtension.onRedeploy.isNotBlank()) {
          args("--on-redeploy", vertxExtension.onRedeploy)
        }
      } else {
        main = vertxExtension.launcher
      }

      vertxExtension.args.forEach { args(it) }
      if (vertxExtension.config.isNotBlank()) {
        args("--conf", vertxExtension.config)
      }
    }

    logger.debug("The vertxRun task has been created")
  }
}


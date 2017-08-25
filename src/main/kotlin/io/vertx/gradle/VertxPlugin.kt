package io.vertx.gradle

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.*
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.ApplicationPluginConvention
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import java.io.File

/**
 * @author [Julien Ponge](https://julien.ponge.org/)
 */
class VertxPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    installVertxExtension(project)
    applyOtherPlugins(project)
    project.gradle.projectsEvaluated {
      defineJavaSourceCompatibility(project)
      defineMainClassName(project)
      configureShadowPlugin(project)
      createVertxRunTask(project)
    }
  }

  private fun installVertxExtension(project: Project) {
    project.extensions.create("vertx", VertxExtension::class.java)
  }

  private fun Project.vertxExtension(): VertxExtension = this.extensions.getByName("vertx") as VertxExtension

  private fun applyOtherPlugins(project: Project) {
    project.pluginManager.apply(JavaPlugin::class.java)
    project.pluginManager.apply(ApplicationPlugin::class.java)
    project.pluginManager.apply(ShadowPlugin::class.java)
  }

  private fun defineJavaSourceCompatibility(project: Project) {
    val javaConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
    javaConvention.sourceCompatibility = JavaVersion.VERSION_1_8
    javaConvention.targetCompatibility = JavaVersion.VERSION_1_8
  }

  private fun defineMainClassName(project: Project) {
    val vertxExtension = project.vertxExtension()
    val applicationConvention = project.convention.getPlugin(ApplicationPluginConvention::class.java)
    applicationConvention.mainClassName = vertxExtension.launcher
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
      main = vertxExtension.launcher

      if (vertxExtension.launcher == "io.vertx.core.Launcher") {
        if (vertxExtension.mainVerticle.isBlank()) {
          throw GradleException("Extension property vertx.mainVerticle must be specified when using io.vertx.core.Launcher as a launcher")
        }
        args("run", vertxExtension.mainVerticle)
      }

      if (vertxExtension.redeploy) {
        args("--launcher-class", "io.vertx.core.Launcher") // FIXME
        args("--redeploy")
        vertxExtension.watch.forEach { args(it) }
        args("--redeploy-grace-period", vertxExtension.redeployGracePeriod)
        args("--redeploy-scan-period", vertxExtension.redeployScanPeriod)
        args("--redeploy-termination-period", vertxExtension.redeployTerminationPeriod)
        if (vertxExtension.onRedeploy.isNotBlank()) {
          args("--on-redeploy", vertxExtension.onRedeploy)
        }
      }

      vertxExtension.args.forEach { args(it) }
      if (vertxExtension.config.isNotBlank()) {
        args("--conf", vertxExtension.config)
      }
    }
  }
}

open class VertxExtension {

  var launcher: String = "io.vertx.core.Launcher"
  var mainVerticle: String = ""

  var args = listOf<String>()
  var config: String = ""
  var workDirectory: String = ""
  var jvmArgs = listOf<String>()

  var redeploy: Boolean = true
  var watch = listOf("src/**/*")
  var onRedeploy: String = "gradle classes" // FIXME
  var redeployScanPeriod: Long = 1000
  var redeployGracePeriod: Long = 1000
  var redeployTerminationPeriod: Long = 1000
}

/*

 = Notes

 Does not play well with daemons

 On redeploy, --launcher-class=(...) combined with `run io.vertx.core.Launcher` does auto-reload of customer
 launcher classes.

 */

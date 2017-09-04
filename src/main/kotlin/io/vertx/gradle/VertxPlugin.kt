package io.vertx.gradle

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.taskdefs.condition.Os
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
 * @author [Julien Ponge](https://julien.ponge.org/)
 */
class VertxPlugin : Plugin<Project> {

  private val logger = LoggerFactory.getLogger(VertxPlugin::class.java)

  override fun apply(project: Project) {
    installVertxExtension(project)
    applyOtherPlugins(project)
    project.gradle.projectsEvaluated {
      logger.debug("Vert.x plugin configuration: ${project.vertxExtension()}")
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
    logger.debug("The plugins needed by the Vert.x plugin have been applied")
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

open class VertxExtension(private val project: Project) {

  var launcher: String = "io.vertx.core.Launcher"
  var mainVerticle: String = ""

  var args = listOf<String>()
  var config: String = ""
  var workDirectory: String = project.projectDir.absolutePath
  var jvmArgs = listOf<String>()

  var redeploy: Boolean = true
  var watch = listOf("${project.projectDir.absolutePath}/src/**/*")
  var onRedeploy: String = findGradleScript()
  var redeployScanPeriod: Long = 1000
  var redeployGracePeriod: Long = 1000
  var redeployTerminationPeriod: Long = 1000

  private fun findGradleScript(): String {
    val gradlewScript = if (Os.isFamily(Os.FAMILY_WINDOWS)) "gradlew.bat" else "gradlew"
    val gradlewScriptFile = File(project.projectDir, gradlewScript)
    return if (gradlewScriptFile.exists()) "${gradlewScriptFile.absolutePath} classes" else "gradle classes"
  }

  override fun toString(): String {
    return "VertxExtension(project=$project, launcher='$launcher', mainVerticle='$mainVerticle', args=$args, config='$config', workDirectory='$workDirectory', jvmArgs=$jvmArgs, redeploy=$redeploy, watch=$watch, onRedeploy='$onRedeploy', redeployScanPeriod=$redeployScanPeriod, redeployGracePeriod=$redeployGracePeriod, redeployTerminationPeriod=$redeployTerminationPeriod)"
  }
}

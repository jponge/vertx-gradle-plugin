package io.vertx.gradle

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import java.io.File

/**
 * Vertx Gradle extension.
 *
 * @author [Julien Ponge](https://julien.ponge.org/)
 */
open class VertxExtension(private val project: Project) {

  var vertxVersion = "3.4.2"

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
    return "VertxExtension(project=$project, vertxVersion='$vertxVersion', launcher='$launcher', mainVerticle='$mainVerticle', args=$args, config='$config', workDirectory='$workDirectory', jvmArgs=$jvmArgs, redeploy=$redeploy, watch=$watch, onRedeploy='$onRedeploy', redeployScanPeriod=$redeployScanPeriod, redeployGracePeriod=$redeployGracePeriod, redeployTerminationPeriod=$redeployTerminationPeriod)"
  }
}

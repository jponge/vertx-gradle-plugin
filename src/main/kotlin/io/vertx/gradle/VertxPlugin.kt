package io.vertx.gradle

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.ApplicationPluginConvention
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention

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
}

open class VertxExtension {
  var launcher: String = "io.vertx.core.Launcher"
  var mainVerticle: String = ""
}

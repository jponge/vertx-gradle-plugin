import org.gradle.api.tasks.wrapper.Wrapper.DistributionType.ALL
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

plugins {
  id("com.gradle.build-scan") version "2.2.1"
  `java-gradle-plugin`
  kotlin("jvm") version "1.3.21"
  id("com.github.ben-manes.versions") version "0.21.0"
  id("com.gradle.plugin-publish") version "0.10.1"
}

fun str2bool(s: String?, v: String = "true"): Boolean {
  return v == s
}

val acceptFile = File(gradle.gradleUserHomeDir, "build-scans/vertx-gradle-plugin/gradle-scans-license-agree.txt")
val env = System.getenv()
val isCI = str2bool(env.get("CI")) || str2bool(env.get("TRAVIS"))
val hasAccepted = isCI || str2bool(env.get("VERTX_GRADLE_SCANS_ACCEPT"), "yes") || acceptFile.exists() && str2bool(acceptFile.readText().trim(), "yes")
val hasRefused = str2bool(env.get("VERTX_GRADLE_SCANS_ACCEPT"), "no") || acceptFile.exists() && str2bool(acceptFile.readText().trim(), "no")

buildScan {
  termsOfServiceUrl   = "https://gradle.com/terms-of-service"
  if (hasAccepted) {
    termsOfServiceAgree = "yes"
  } else if (!hasRefused) {
    gradle.buildFinished {
      println("""
This build uses Gradle Build Scans to gather statistics, share information about
failures, environmental issues, dependencies resolved during the build and more.
Build scans will be published after each build, if you accept the terms of
service, and in particular the privacy policy.

Please read

    https://gradle.com/terms-of-service
    https://gradle.com/legal/privacy

and then:

  - set the `VERTX_GRADLE_SCANS_ACCEPT` to `yes`/`no` if you agree with/refuse the TOS
  - or create the ${acceptFile} file with `yes`/`no` in it if you agree with/refuse

And we'll not bother you again. Note that build scans are only made public if
you share the URL at the end of the build.
""")
    }
  }
}

repositories {
  jcenter()
  mavenLocal()
}

version = "0.5.0-SNAPSHOT"
group = "io.vertx"

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  implementation("com.github.jengelman.gradle.plugins:shadow:5.0.0")
  implementation("com.netflix.nebula:nebula-dependency-recommender:7.5.2") {
    exclude(group = "org.jetbrains.kotlin")
  }

  testImplementation("junit:junit:4.12")
  testImplementation("com.mashape.unirest:unirest-java:1.4.9")
  testImplementation("org.assertj:assertj-core:3.12.1")
}

gradlePlugin {
  plugins {
    create("vertxPlugin") {
      id = "io.vertx.vertx-plugin"
      implementationClass = "io.vertx.gradle.VertxPlugin"
    }
  }
}

pluginBundle {
  website = "https://github.com/jponge/vertx-gradle-plugin"
  vcsUrl = "https://github.com/jponge/vertx-gradle-plugin"
  plugins {
    getByName("vertxPlugin") {
      id = "io.vertx.vertx-plugin"
      displayName = "Vert.x Gradle Plugin"
      description = "An opinionated Gradle plugin for Vert.x projects"
      tags = listOf("vertx", "vert.x", "reactive", "microservice")
    }
  }
}

tasks {
  getByName<Wrapper>("wrapper") {
    gradleVersion = "5.2.1"
    distributionType = ALL
  }

  withType<KotlinCompile> {
    kotlinOptions { jvmTarget = "1.8" }
  }
}

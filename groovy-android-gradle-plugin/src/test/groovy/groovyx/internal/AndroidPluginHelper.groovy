/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package groovyx.internal

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import groovyx.GroovyAndroidExtension
import groovyx.GroovyAndroidPlugin
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

trait AndroidPluginHelper {
  void applyAppPlugin() {
    project.pluginManager.apply(AppPlugin)
    createSourceDirs(project.extensions.getByName('android').sourceSets)
    project.pluginManager.apply(GroovyAndroidPlugin)
  }

  void applyLibraryPlugin() {
    project.pluginManager.apply(LibraryPlugin)
    createSourceDirs(project.extensions.getByName('android').sourceSets)
    project.pluginManager.apply(GroovyAndroidPlugin)
  }

  GroovyAndroidExtension getExtension() {
    project.extensions.getByType(GroovyAndroidExtension)
  }

  void createSourceDirs(NamedDomainObjectContainer<SourceSet> sourceSet){
    sourceSet.each {
      project.file("src/$it.name/groovy").mkdirs()
    }
  }

  abstract Project getProject()
}

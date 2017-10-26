/*
 * Copyright 2017 the original author or authors.
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

package groovyx

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.internal.VariantManager
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.builder.model.SourceProvider
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.internal.HasConvention
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.tasks.GroovySourceSet
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.internal.reflect.Instantiator

import javax.inject.Inject
/**
 * Adds support for building Android applications using the Groovy language.
 */
@Slf4j
class GroovyAndroidPlugin implements Plugin<Project> {

  public static final String ANDROID_GROOVY_EXTENSION_NAME = 'androidGroovy'

  private Project project
  private GroovyAndroidExtension extension

  private final Instantiator instantiator

  @Inject GroovyAndroidPlugin(Instantiator instantiator) {
    this.instantiator = instantiator
  }

  void apply(Project project) {
    this.project = project

    BasePlugin basePlugin = getAndroidBasePlugin(project)
    if (!basePlugin) {
        throw new GradleException('You must apply the Android plugin or the Android library plugin before using the groovy-android plugin')
    }

    extension = project.extensions.create(ANDROID_GROOVY_EXTENSION_NAME, GroovyAndroidExtension, project, instantiator, fileResolver)

    androidExtension.sourceSets.all { AndroidSourceSet sourceSet ->
      if (!(sourceSet instanceof HasConvention)) {
        return
      }

      def sourceSetName = sourceSet.name
      def sourceSetPath = project.file("src/$sourceSetName/groovy")

      if (!sourceSetPath.exists()) {
        log.debug('SourceSet path does not exists for {} {}', sourceSetName, sourceSetPath)
        return
      }

      // add so Android Studio will recognize groovy files can see these
      sourceSet.java.srcDir(sourceSetPath)

      // create groovy source set so we can access it later
      def groovySourceSet = extension.sourceSetsContainer.maybeCreate(sourceSetName)
      sourceSet.convention.plugins['groovy'] = groovySourceSet
      def groovyDirSet = groovySourceSet.groovy
      groovyDirSet.srcDir(sourceSetPath)

      log.debug('Created groovy sourceDirectorySet at {}', groovyDirSet.srcDirs)
    }

    project.afterEvaluate { Project afterProject ->
      def androidPlugin = getAndroidBasePlugin(afterProject)
      def variantManager = getVariantManager(androidPlugin)
      processVariantData(variantManager.variantDataList, androidExtension, androidPlugin)
    }
  }

  private void processVariantData(
    List<BaseVariantData> variantDataList,
    BaseExtension androidExtension, BasePlugin androidPlugin) {

    variantDataList.each { variantData ->
      def variantDataName = variantData.name
      log.debug('Process variant {}', variantDataName)

      def javaTask = getJavaTask(variantData)
      if (javaTask == null) {
        log.info('javaTask is missing for {}, so Groovy files won\'t be compiled for it', variantDataName)
        return
      }

      def taskName = javaTask.name.replace('Java', 'Groovy')
      def groovyTask = project.tasks.create(taskName, GroovyCompile)

      // do before configuration so users can override / don't break backwards compatibility
      groovyTask.targetCompatibility = javaTask.targetCompatibility
      groovyTask.sourceCompatibility = javaTask.sourceCompatibility
      extension.configure(groovyTask)

      groovyTask.destinationDir = javaTask.destinationDir
      groovyTask.description = "Compiles the $variantDataName in groovy."
      groovyTask.classpath = javaTask.classpath
      groovyTask.setDependsOn(javaTask.dependsOn)
      groovyTask.groovyClasspath = javaTask.classpath

      def providers = variantData.variantConfiguration.sortedSourceProviders
      providers.each { SourceProvider provider ->
        def groovySourceSet = provider.convention.plugins['groovy'] as GroovySourceSet
        if (groovySourceSet == null) {
          return
        }

        def groovySourceDirectorySet = groovySourceSet.groovy
        groovyTask.source(groovySourceDirectorySet)

        // Exclude any java files that may be included in both java and groovy source sets
        javaTask.exclude { file ->
          file.file in groovySourceSet.groovy.files
        }

        if (extension.skipJavaC) {
          groovyTask.source(*(javaTask.source.files as List))
          javaTask.exclude {
            true
          }
        }
      }

      // no sources for groovy to compile skip the groovy task
      if (groovyTask.source.empty) {
        log.debug('no groovy sources found for {} removing groovy task', variantDataName)
        project.tasks.remove(groovyTask)
        return
      }
      log.debug('groovy sources for {}: {}', variantDataName, groovyTask.source.files)

      def additionalSourceFiles = getGeneratedSourceDirs(variantData)
      groovyTask.source(*additionalSourceFiles)

      groovyTask.doFirst { GroovyCompile task ->
        def androidRunTime = project.files(getRuntimeJars(androidPlugin, androidExtension))
        def kotlinFiles = project.files(getKotlinClassFiles(variantDataName))
        task.classpath = androidRunTime + javaTask.classpath + kotlinFiles
        task.groovyClasspath = task.classpath
        task.options.compilerArgs += javaTask.options.compilerArgs
        task.groovyOptions.javaAnnotationProcessing = true
      }

      log.debug('Groovy classpath: {}', groovyTask.classpath.files)
      log.debug('Groovy compiler args {}', groovyTask.options.compilerArgs)

      javaTask.finalizedBy(groovyTask)
    }
  }

  @CompileStatic
  private static BasePlugin getAndroidBasePlugin(Project project) {
    def plugin = project.plugins.findPlugin('android') ?:
        project.plugins.findPlugin('android-library')

    return plugin as BasePlugin
  }

  @CompileStatic
  private BaseExtension getAndroidExtension() {
    return project.extensions.getByName('android') as BaseExtension
  }

  private static getRuntimeJars(BasePlugin plugin, BaseExtension extension) {
    if (plugin.metaClass.getMetaMethod('getRuntimeJarList')) {
      return plugin.runtimeJarList
    }

    if (extension.metaClass.getMetaMethod('getBootClasspath')) {
      return extension.bootClasspath
    }

    return plugin.bootClasspath
  }

  private static SourceTask getJavaTask(BaseVariantData baseVariantData) {
    if (baseVariantData.metaClass.getMetaProperty('javaCompileTask')) {
      return baseVariantData.javaCompileTask
    }

    if (baseVariantData.metaClass.getMetaProperty('javaCompilerTask')) {
      return baseVariantData.javaCompilerTask
    }

    return null
  }

  private static List<File> getGeneratedSourceDirs(BaseVariantData variantData) {
    List<File> result = []

    def getExtraSourcesMethod = variantData.getMetaClass().getMetaMethod("getExtraGeneratedSourceFolders")
    if (getExtraSourcesMethod.returnType.metaClass == List.metaClass) {
      def folders = variantData.getExtraGeneratedSourceFolders()
      if (folders != null) {
        result.addAll(folders)
      }
    }

    def getJavaSourcesMethod = variantData.metaClass.getMetaMethod('getJavaSources')
    if (getJavaSourcesMethod.returnType.metaClass == List.metaClass) {
      def fileTrees = variantData.javaSources.findAll { it instanceof ConfigurableFileTree }
      result.addAll(fileTrees.collect { it.getDir() })
      return result
    }

    if (getJavaSourcesMethod.returnType.metaClass == Object[].metaClass) {
      def sources = variantData.javaSources
      result.addAll(sources.findAll { it instanceof File } as Collection<File>)
      return result
    }

    if (variantData.scope.generateRClassTask != null) {
      result << variantData.scope.RClassSourceOutputDir
    }

    if (variantData.scope.generateBuildConfigTask != null) {
      result << variantData.scope.buildConfigSourceOutputDir
    }

    if (variantData.scope.getAidlCompileTask() != null) {
      result << variantData.scope.aidlSourceOutputDir
    }

    if (variantData.scope.annotationProcessorOutputDir != null) {
      result << variantData.scope.annotationProcessorOutputDir
    }

    // We use getter instead of property for globalScope since property returns
    // TransformGlobalScope instead of GlobalScope (Static type checker failing).
    if (variantData.scope.getGlobalScope().extension.dataBinding.enabled) {
      result << variantData.scope.classOutputForDataBinding
    }

    if (!variantData.variantConfiguration.renderscriptNdkModeEnabled
        && variantData.scope.renderscriptCompileTask != null) {
      result << variantData.scope.renderscriptSourceOutputDir
    }

    return result
  }

  private FileResolver getFileResolver() {
    return project.fileResolver
  }

  private static VariantManager getVariantManager(BasePlugin plugin) {
    return plugin.variantManager
  }

  private getKotlinClassFiles(String variantName) {
    // kotlin plugin exists
    def kotlin = project.plugins.findPlugin('kotlin-android')
    if (kotlin != null) {
      def kotlinCopyTask = project.tasks.findByName("copy${variantName.capitalize()}KotlinClasses")
      return kotlinCopyTask.kotlinOutputDir
    }

    // empty list to avoid null issues when adding to FileCollection
    return Collections.emptyList()
  }
}

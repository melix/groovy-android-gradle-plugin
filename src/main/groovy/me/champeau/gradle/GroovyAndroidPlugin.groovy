package me.champeau.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.GroovyCompile

/**
 * This is the main plugin file. Put a description of your plugin here.
 */
class GroovyAndroidPlugin implements Plugin<Project> {

    private static List RUNTIMEJARS_COMPAT = [
            { it.runtimeJars },
            { it.bootClasspath }
    ]

    void apply(Project project) {

        def androidPlugin = project.plugins.hasPlugin('android')
        if (!androidPlugin) {
            throw new GradleException('You must apply the Android plugin before using the groovy-android plugin')
        }

        def groovyPlugin = this
        project.android {

            packagingOptions {
                // workaround for http://stackoverflow.com/questions/20673625/android-gradle-plugin-0-7-0-duplicate-files-during-packaging-of-apk
                exclude 'META-INF/LICENSE.txt'
                exclude 'META-INF/groovy-release-info.properties'
            }

            applicationVariants.all {
                project.task("groovy${name}Compile",type: GroovyCompile) {
                    source = javaCompile.source + project.fileTree('src/main/java').include('**/*.groovy') +
                            project.fileTree('src/main/groovy').include('**/*.groovy')
                    destinationDir = javaCompile.destinationDir
                    classpath = javaCompile.classpath
                    groovyClasspath = classpath
                    sourceCompatibility = '1.6'
                    targetCompatibility = '1.6'
                    doFirst {
                        def runtimeJars = groovyPlugin.getRuntimeJars(project, project.plugins.find { it.class.name == 'com.android.build.gradle.AppPlugin' })
                        classpath = project.files(runtimeJars) + classpath
                    }
                }
                javaCompile.dependsOn("groovy${name}Compile")
                javaCompile.enabled = false
            }
        }
        project.logger.info("Detected Android plugin version ${getAndroidPluginVersion(project)}")
    }

    private String getAndroidPluginVersion(Project project) {
        try {
            return project.buildscript.configurations.classpath.resolvedConfiguration.firstLevelModuleDependencies.find { it.moduleGroup == 'com.android.tools.build' &&  it.moduleName=='gradle' }.moduleVersion
        } catch (e) {
            return null
        }
    }

    def getRuntimeJars(Project project, plugin) {
        int index
        switch (getAndroidPluginVersion(project)) {
            case ~/0\.9\..*/:
                index = 0
                break
            case ~/0\.10\..*/:
                index = 1
                break
            case ~/0\.11\..*/:
                index = 1
                break
            default:
                index = RUNTIMEJARS_COMPAT.size()-1
        }
        def fun = RUNTIMEJARS_COMPAT[index]
        fun(plugin)
    }
}

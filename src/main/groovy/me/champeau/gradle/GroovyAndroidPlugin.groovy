package me.champeau.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.GroovyCompile

/**
 * This is the main plugin file. Put a description of your plugin here.
 */
class GroovyAndroidPlugin implements Plugin<Project> {
    void apply(Project project) {

        def androidPlugin = project.plugins.hasPlugin('android')
        if (!androidPlugin) {
            throw new GradleException('You must apply the Android plugin before using the groovy-android plugin')
        }

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
                        def runtimeJars = project.plugins.findPlugin("android").bootClasspath
                        classpath = project.files(runtimeJars) + classpath
                    }
                }
                javaCompile.dependsOn("groovy${name}Compile")
                javaCompile.enabled = false
            }

        }


    }
}

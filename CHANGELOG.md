1.3.0
-----

* dropped offical support for anything less that 2.2.0 version of android gradle plugin and 3.0 gradle

1.2.0
-----
    * Added support for annotation processing (#142)
    * Disabled groovy task for when there are no groovy sources
    * Kotlin Support - Kotlin plugin applied before Groovy plugin will allow the Groovy compiler
    to know about the Kotlin source files. (#139)
    * Dropped (offical) support for Android Gradle Plugin Versions < 1.5.0. These plugins should 
    continue to work but no intentional tests are being run to ensure continuing support in future
    releases.
1.1.0
-----
    * Support for 2.2.0 of Android Gradle Plugin
    * Removed direct dependency on Android Gradle Plugin (#127)
1.0.0
-----
    * Renamed plugin id from 'groovyx.grooid.groovy-android' to 'groovyx.android'
    * Renamed plugin name from 'groovy-android-gradle-plugin' to 'gradle-groovy-android-plugin' 
0.3.10
------
    * Fixed problem with the plugin being applied to projects using Gradle 2.12
0.3.9
-----
    * GroovyCompile's targetCompatibility and sourceCompatibility are automatically set 
      based off of JavaCompile's
    * Custom groovy sourceSets added. Java directories may be used to java be joint 
      compiled with groovy 
    * Added flag in androidGroovy to skip JavaCompile and have GroovyCompile do all
      compilation.
0.3.8
-----
    * Fixes issues for users not on Java 8. Supports Java 6+
0.3.7
-----
    * Support for Android Gradle plugin 1.2.0 - 1.5.0
0.3.6
-----
    * Support for Android Gradle plugin 1.1.0
0.3.5
-----
    * Moved to Groovy organization
    * renamed from 'me.champeau.gradle.groovy-android' to 'groovyx.grooid.groovy-android'
    * Support for plugin version 0.14
    * Support for plugin version 1.0.0
    * Add support for build type specific source directory
0.3.4
-----
    * Support for product flavors
0.3.3
-----
    * Fix javaCompile task referencing Groovy sources
0.3.2
-----
    * Add support for Groovy compilation options
    * Fix joint compilation bug
    * Sources need to be in 'src/main/groovy'
0.3.1
-----
    * Make sure the plugin works with Android Tools 0.13+
    * support for androidTest
    * add the "groovy" sourceSet
0.3.0
-----
    * Support for Android library plugin

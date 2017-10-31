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

package groovyx.functional

import groovyx.functional.internal.AndroidFunctionalSpec
import spock.lang.IgnoreIf
import spock.lang.Unroll

import static groovyx.internal.TestProperties.*
/**
 * Complete test suite to ensure the plugin works with the different versions of android gradle plugin.
 * This will only be run if the system property of 'allTests' is set to true
 */
@IgnoreIf({ !allTests })
class FullCompilationSpec extends AndroidFunctionalSpec {

  @Unroll
  def "should compile android app with java:#javaVersion, android plugin:#androidPluginVersion, gradle version: #gradleVersion"() {
    given:
    file("settings.gradle") << "rootProject.name = 'test-app'"

    createBuildFileForApplication(_androidPluginVersion, javaVersion)
    createAndroidManifest()
    createMainActivityLayoutFile()

    // create Java class to ensure this compile correctly along with groovy classes
    file('src/main/java/groovyx/test/SimpleJava.java') << """
      package groovyx.test;

      public class SimpleJava {
        public static int getInt() {
          return 1337;
        }
      }
    """

    // create Java class in groovy folder to ensure this compile correctly along with groovy classes
    file('src/main/groovy/groovyx/test/SimpleJavaGroovy.java') << """
      package groovyx.test;

      public class SimpleJavaGroovy {
        public static int getInt() {
          return 2;
        }
      }
    """

    file('src/main/groovy/groovyx/test/MainActivity.groovy') << """
      package groovyx.test

      import android.app.Activity
      import android.os.Bundle
      import groovy.transform.CompileStatic

      @CompileStatic
      class MainActivity extends Activity {
        @Override void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState)
          contentView = R.layout.activity_main

          def someValue = SimpleJava.int
          def result = someValue * SimpleJavaGroovy.int
        }
      }
    """

    file('src/androidTest/groovy/groovyx/test/AndroidTest.groovy') << """
      package groovyx.test

      import android.support.test.runner.AndroidJUnit4
      import android.test.suitebuilder.annotation.SmallTest
      import groovy.transform.CompileStatic
      import org.junit.Before
      import org.junit.Test
      import org.junit.runner.RunWith

      @RunWith(AndroidJUnit4)
      @SmallTest
      @CompileStatic
      class AndroidTest {
        @Test void shouldCompile() {
          assert 5 * 2 == 10
        }
      }
    """

    file('src/test/groovy/groovyx/test/JvmTest.groovy') << """
      package groovyx.test

      import org.junit.Test

      class JvmTest {
        @Test void shouldCompile() {
          assert 10 * 2 == 20
        }
      }
    """

    when:
    runWithVersion gradleVersion, 'assemble', 'test'

    then:
    noExceptionThrown()
    file('build/outputs/apk/test-app-debug.apk').exists()
    file('build/intermediates/classes/debug/groovyx/test/MainActivity.class').exists()
    file('build/intermediates/classes/androidTest/debug/groovyx/test/AndroidTest.class').exists()
    file('build/intermediates/classes/test/debug/groovyx/test/JvmTest.class').exists()
    file('build/intermediates/classes/test/release/groovyx/test/JvmTest.class').exists()

    where:
    // test common configs that touches the different way to access the classpath
    javaVersion                     | _androidPluginVersion | gradleVersion
    'JavaVersion.VERSION_1_7'       | '2.2.0'              | '3.0'
    'JavaVersion.VERSION_1_7'       | '2.2.0'              | '3.1'
    'JavaVersion.VERSION_1_6'       | '2.2.3'              | '3.2'
    'JavaVersion.VERSION_1_7'       | '2.3.0'              | '3.3'
    'JavaVersion.VERSION_1_7'       | '2.3.0'              | '3.4'
    'JavaVersion.VERSION_1_7'       | '2.3.1'              | '3.5'
    'JavaVersion.VERSION_1_7'       | '2.3.2'              | '3.5'
    'JavaVersion.VERSION_1_7'       | '2.3.3'              | '4.2'
  }

  @Unroll
  def "should compile android library with java:#javaVersion and android plugin:#androidPluginVersion, gradle version:#gradleVersion"() {
    given:
    file("settings.gradle") << "rootProject.name = 'test-lib'"

    buildFile << """
      buildscript {
        repositories {
          maven { url "${localRepo.toURI()}" }
          jcenter()
          google()
        }
        dependencies {
          classpath 'com.android.tools.build:gradle:$_androidPluginVersion'
          classpath 'org.codehaus.groovy:groovy-android-gradle-plugin:$PLUGIN_VERSION'
        }
      }

      apply plugin: 'com.android.library'
      apply plugin: 'groovyx.android'

      repositories {
        jcenter()
        google()
      }

      android {
        compileSdkVersion $compileSdkVersion
        buildToolsVersion '$buildToolsVersion'

        defaultConfig {
          minSdkVersion 16
          targetSdkVersion $compileSdkVersion

          versionCode 1
          versionName '1.0.0'
        }

        compileOptions {
          sourceCompatibility $javaVersion
          targetCompatibility $javaVersion
        }
      }

      dependencies {
        compile 'org.codehaus.groovy:groovy:2.4.12:grooid'

        androidTestCompile 'com.android.support.test:runner:0.4.1'
        androidTestCompile 'com.android.support.test:rules:0.4.1'

        testCompile 'junit:junit:4.12'
      }

      // force unit test types to be assembled too
      android.testVariants.all { variant ->
        tasks.getByName('assemble').dependsOn variant.assemble
      }
    """

    file('src/main/AndroidManifest.xml') << """
      <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        package="groovyx.test"/>
    """

    // create Java class to ensure this compiles correctly along with groovy classes
    file('src/main/java/groovyx/test/SimpleJava.java') << """
      package groovyx.test;

      public class SimpleJava {
        public static int getInt() {
          return 1;
        }
      }
    """

    // create Java class in groovy folder to ensure this compile correctly along with groovy classes
    file('src/main/groovy/groovyx/test/SimpleJavaGroovy.java') << """
      package groovyx.test;

      public class SimpleJavaGroovy {
        public static int getInt() {
          return 2;
        }
      }
    """

    file('src/main/groovy/groovyx/test/Test.groovy') << """
      package groovyx.test

      import android.util.Log
      import groovy.transform.CompileStatic

      @CompileStatic
      class Test {
        static void testMethod() {
          Log.d(Test.name, "Testing \${SimpleJava.int} \${SimpleJavaGroovy.int}")
        }
      }
    """

    file('src/androidTest/groovy/groovyx/test/AndroidTest.groovy') << """
      package groovyx.test

      import android.support.test.runner.AndroidJUnit4
      import android.test.suitebuilder.annotation.SmallTest
      import groovy.transform.CompileStatic
      import org.junit.Before
      import org.junit.Test
      import org.junit.runner.RunWith

      @RunWith(AndroidJUnit4)
      @SmallTest
      @CompileStatic
      class AndroidTest {
        @Test
        void shouldCompile() {
          assert 5 == 5
        }
      }
    """

    file('src/test/groovy/groovyx/test/JvmTest.groovy') << """
      package groovyx.test

      import org.junit.Test

      class JvmTest {
        @Test void shouldCompile() {
          assert 10 * 2 == 20
        }
      }
    """

    when:
    runWithVersion gradleVersion, 'assemble', 'test'

    then:
    noExceptionThrown()
    file('build/outputs/aar/test-lib-debug.aar').exists()
    file('build/outputs/aar/test-lib-release.aar').exists()
    file('build/intermediates/classes/debug/groovyx/test/Test.class').exists()
    file('build/intermediates/classes/release/groovyx/test/Test.class').exists()
    file('build/intermediates/classes/androidTest/debug/groovyx/test/AndroidTest.class').exists()
    file('build/intermediates/classes/test/debug/groovyx/test/JvmTest.class').exists()
    file('build/intermediates/classes/test/release/groovyx/test/JvmTest.class').exists()

    where:
    // test common configs that touches the different way to access the classpath
    javaVersion                     | _androidPluginVersion | gradleVersion
    'JavaVersion.VERSION_1_6'       | '1.5.0'              | '2.10'
    'JavaVersion.VERSION_1_7'       | '1.5.0'              | '2.11'
    'JavaVersion.VERSION_1_7'       | '1.5.0'              | '2.12'
    'JavaVersion.VERSION_1_7'       | '2.0.0'              | '2.13'
    'JavaVersion.VERSION_1_7'       | '2.1.2'              | '2.14'
    'JavaVersion.VERSION_1_7'       | '2.2.0'              | '2.14.1'
    'JavaVersion.VERSION_1_7'       | '2.2.0'              | '3.0'
    'JavaVersion.VERSION_1_7'       | '2.2.0'              | '3.1'
    'JavaVersion.VERSION_1_6'       | '2.2.3'              | '3.2'
    'JavaVersion.VERSION_1_7'       | '2.3.0'              | '3.3'
    'JavaVersion.VERSION_1_7'       | '2.3.0'              | '3.4'
    'JavaVersion.VERSION_1_7'       | '2.3.1'              | '3.5'
    'JavaVersion.VERSION_1_7'       | '2.3.1'              | '3.5'
    'JavaVersion.VERSION_1_7'       | '2.3.3'              | '4.2'
  }
}

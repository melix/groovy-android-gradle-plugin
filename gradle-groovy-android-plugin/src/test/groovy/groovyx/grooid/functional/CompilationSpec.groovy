package groovyx.grooid.functional

import spock.lang.Unroll

class CompilationSpec extends FunctionalSpec {

  @Unroll
  def "should compile android app with java:#javaVersion, android plugin:#androidPluginVersion"() {
    given:
    file("settings.gradle") << "rootProject.name = 'test-app'"

    buildFile << """
      buildscript {
        repositories {
          maven { url "${localRepo.toURI()}" }
          jcenter()
        }
        dependencies {
          classpath 'com.android.tools.build:gradle:$androidPluginVersion'
          classpath 'org.codehaus.groovy:gradle-groovy-android-plugin:$PLUGIN_VERSION'
        }
      }

      apply plugin: 'com.android.application'
      apply plugin: 'groovyx.grooid.groovy-android'

      repositories {
        jcenter()
      }

      android {
        compileSdkVersion 23
        buildToolsVersion "21.1.2"

        defaultConfig {
          minSdkVersion 16
          targetSdkVersion 23

          versionCode 1
          versionName '1.0.0'
        }

        buildTypes {
          debug {
            applicationIdSuffix '.dev'
          }
        }

        compileOptions {
          sourceCompatibility $javaVersion
          targetCompatibility $javaVersion
        }
      }

      dependencies {
        compile 'org.codehaus.groovy:groovy:2.4.5:grooid'
      }
    """

    file('src/main/AndroidManifest.xml') << """
      <?xml version="1.0" encoding="utf-8"?>
      <manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="groovyx.grooid.test">

        <application
            android:allowBackup="true"
            android:label="Test App">
          <activity
              android:name=".MainActivity"
              android:label="Test App">
            <intent-filter>
              <action android:name="android.intent.action.MAIN"/>
              <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
          </activity>
        </application>

      </manifest>
    """.trim()

    file('src/main/res/layout/activity_main.xml') << """
      <?xml version="1.0" encoding="utf-8"?>
      <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
      >

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Hello Groovy!"
            android:gravity="center"
            android:textAppearance="?android:textAppearanceLarge"
        />

      </FrameLayout>
    """.trim()

    file('src/main/groovy/groovyx/grooid/test/MainActivity.groovy') << """
      package groovyx.grooid.test

      import android.app.Activity
      import android.os.Bundle
      import groovy.transform.CompileStatic

      @CompileStatic
      class MainActivity extends Activity {
        @Override void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState)
          contentView = R.layout.activity_main
        }
      }
    """

    when:
    runWithVersion gradleVersion, 'assemble'


    then:
    noExceptionThrown()
    file('build/outputs/apk/test-app-debug.apk').exists()

    where:
    // test common configs that touches the different way to access the classpath
    javaVersion | androidPluginVersion | gradleVersion
    '1.6'       | '1.3.0' | '2.2' // android plugin requires 2.2.
    '1.6'       | '1.5.0' | '2.10'
    '1.7'       | '1.5.0' | '2.10'
  }

  @Unroll
  def "should compile android library with java:#javaVersion and android plugin:#androidPluginVersion"() {
    given:
    file("settings.gradle") << "rootProject.name = 'test-lib'"

    buildFile << """
      buildscript {
        repositories {
          maven { url "${localRepo.toURI()}" }
          jcenter()
        }
        dependencies {
          classpath 'com.android.tools.build:gradle:$androidPluginVersion'
          classpath 'org.codehaus.groovy:gradle-groovy-android-plugin:$PLUGIN_VERSION'
        }
      }

      apply plugin: 'com.android.library'
      apply plugin: 'groovyx.grooid.groovy-android'

      repositories {
        jcenter()
      }

      android {
        compileSdkVersion 23
        buildToolsVersion "21.1.2"

        defaultConfig {
          minSdkVersion 16
          targetSdkVersion 23

          versionCode 1
          versionName '1.0.0'
        }

        compileOptions {
          sourceCompatibility $javaVersion
          targetCompatibility $javaVersion
        }
      }

      dependencies {
        compile 'org.codehaus.groovy:groovy:2.4.5:grooid'
      }
    """

    file('src/main/AndroidManifest.xml') << '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="groovyx.grooid.test"/>'

    file('src/main/groovyx/grooid/test/Test.groovy') << """
      package groovyx.grooid.test

      import groovy.transform.CompileStatic

      @CompileStatic
      class Test {
        static void TestMethod() {
          println 'test method!'
        }
      }
    """

    when:
    runWithVersion gradleVersion, 'assemble'

    then:
    noExceptionThrown()
    file('build/outputs/aar/test-lib-debug.aar').exists()
    file('build/outputs/aar/test-lib-release.aar').exists()

    where:
    // test common configs that touches the different way to access the classpath
    javaVersion | androidPluginVersion | gradleVersion
    '1.6'       | '1.3.0' | '2.2' // android plugin requires 2.2.
    '1.6'       | '1.5.0' | '2.10'
    '1.7'       | '1.5.0' | '2.10'
  }
}

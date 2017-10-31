package groovyx.functional.internal

import groovyx.internal.TestProperties

import static groovyx.internal.TestProperties.buildToolsVersion
import static groovyx.internal.TestProperties.compileSdkVersion

abstract class AndroidFunctionalSpec extends FunctionalSpec {

  enum AndroidPlugin {
    LIBRARY('com.android.library'),
    APP('com.android.application')

    private final String value

    AndroidPlugin(String value) {
      this.value = value
    }
  }

  void createBuildFileForApplication(String androidPluginVersion = TestProperties.androidPluginVersion,
                                     String javaVersion = 'JavaVersion.VERSION_1_7') {
    createBuildFile(AndroidPlugin.APP, androidPluginVersion, javaVersion)
  }

  void createBuildFileForLibrary(String androidPluginVersion = TestProperties.androidPluginVersion,
                                     String javaVersion = 'JavaVersion.VERSION_1_7') {
    createBuildFile(AndroidPlugin.LIBRARY, androidPluginVersion, javaVersion)
  }

  void createBuildFile(AndroidPlugin plugin, String androidPluginVersion = TestProperties.androidPluginVersion,
                       String javaVersion = 'JavaVersion.VERSION_1_7') {
    createProguardRules()

    buildFile << """
      buildscript {
        repositories {
          maven { url "${localRepo.toURI()}" }
          jcenter()
          google()
        }
        dependencies {
          classpath 'com.android.tools.build:gradle:$androidPluginVersion'
          classpath 'org.codehaus.groovy:groovy-android-gradle-plugin:$PLUGIN_VERSION'
        }
      }

      apply plugin: '$plugin.value'
      apply plugin: 'groovyx.android'

      repositories {
        jcenter()
        google()
      }

      android {
        compileSdkVersion $compileSdkVersion
        buildToolsVersion '$buildToolsVersion'

        defaultConfig {
          minSdkVersion ${plugin == AndroidPlugin.LIBRARY ? '26' : '16' }
          targetSdkVersion $compileSdkVersion

          versionCode 1
          versionName '1.0.0'

          testInstrumentationRunner 'android.support.test.runner.AndroidJUnitRunner'
        }

        buildTypes {
          debug {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.txt'
            testProguardFile 'proguard-rules.txt'
          }
          release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.txt'
          }
        }

        compileOptions {
          sourceCompatibility $javaVersion
          targetCompatibility $javaVersion
        }
      }

      dependencies {
        implementation 'org.codehaus.groovy:groovy:2.4.12:grooid'

        implementation 'com.squareup.moshi:moshi:1.5.0'
        annotationProcessor 'com.ryanharter.auto.value:auto-value-moshi:0.4.3'
        compileOnly 'com.ryanharter.auto.value:auto-value-moshi-annotations:0.4.3'

        annotationProcessor 'com.google.auto.value:auto-value:1.4.1'
        compileOnly 'com.jakewharton.auto.value:auto-value-annotations:1.4'

        androidTestImplementation 'com.android.support.test:runner:1.0.1'
        androidTestImplementation 'com.android.support.test:rules:1.0.1'

        testImplementation 'junit:junit:4.12'
      }

      configurations.all {
        resolutionStrategy.force 'com.android.support:support-annotations:26.1.0'
      }

      // force unit test types to be assembled too
      android.testVariants.all { variant ->
        tasks.getByName('assemble').dependsOn variant.assemble
      }
    """
  }

  void createProguardRules() {
    file('proguard-rules.txt') << """
      -dontobfuscate
      
      -keep class org.codehaus.groovy.vmplugin.**
      -keep class org.codehaus.groovy.runtime.dgm*
      
      -keepclassmembers class org.codehaus.groovy.runtime.dgm* {*;}
      -keepclassmembers class ** implements org.codehaus.groovy.runtime.GeneratedClosure {*;}
      -keepclassmembers class org.codehaus.groovy.reflection.GroovyClassValue* {*;}
      
      
      -dontwarn org.codehaus.groovy.**
      -dontwarn groovy**
      
      -keep class org.xmlpull.v1.**
      -dontwarn org.xmlpull.v1.**
      """
  }

  void createAndroidManifest() {
    file('src/main/AndroidManifest.xml') << """
      <?xml version="1.0" encoding="utf-8"?>
      <manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="groovyx.test">

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
  }

  void createMainActivityLayoutFile() {
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
  }
}

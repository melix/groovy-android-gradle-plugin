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

package groovyx.grooid.functional

import spock.lang.IgnoreIf

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

import static groovyx.grooid.internal.TestProperties.allTests
import static groovyx.grooid.internal.TestProperties.androidPluginVersion
import static groovyx.grooid.internal.TestProperties.buildToolsVersion
import static groovyx.grooid.internal.TestProperties.compileSdkVersion

/**
 * Ensure that projects with renderscript compile properly.
 */
@IgnoreIf({ !allTests})
class RenderScriptCompilationSpec extends FunctionalSpec {

  def "should compile with renderscript"() {
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
        compileSdkVersion $compileSdkVersion
        buildToolsVersion '$buildToolsVersion'

        defaultConfig {
          minSdkVersion 16
          targetSdkVersion $compileSdkVersion

          versionCode 1
          versionName '1.0.0'

          renderscriptTargetApi 20
          renderscriptSupportModeEnabled true
        }

        buildTypes {
          debug {
            applicationIdSuffix '.dev'
          }
        }

        compileOptions {
          sourceCompatibility JavaVersion.VERSION_1_7
          targetCompatibility JavaVersion.VERSION_1_7
        }
      }

      dependencies {
        compile 'org.codehaus.groovy:groovy:2.4.5:grooid'
      }
    """

    def image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
    for (x in 0..99) {
      for (y in 0..99) {
        int color = (x + y) % 2 == 0 ? 0x000000 : 0xffffff
        image.setRGB(x, y, color)
      }
    }

    def testImage = file('src/main/res/drawable-xhdpi/test.png')
    ImageIO.write(image, 'png', testImage)

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
      import android.graphics.Bitmap
      import android.graphics.BitmapFactory
      import android.os.Bundle
      import android.support.v8.renderscript.Allocation
      import android.support.v8.renderscript.Element
      import android.support.v8.renderscript.RenderScript
      import android.support.v8.renderscript.ScriptIntrinsicBlur
      import groovy.transform.CompileStatic

      @CompileStatic
      class MainActivity extends Activity {
        @Override void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState)
          contentView = R.layout.activity_main

          def renderScript = RenderScript.create(this);
          def source = BitmapFactory.decodeResource(resources, R.drawable.test)

          def input = Allocation.createFromBitmap(
                renderScript, source, Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT)

          def output = Allocation.createTyped(renderScript, input.type)

          def script = ScriptIntrinsicBlur.create(renderScript,
                Element.U8_4(renderScript))
          script.setRadius(25)
          script.setInput(input)
          script.forEach(output)

          def bitmap = Bitmap.createBitmap(source.width, source.height, source.config)
          output.copyTo(bitmap)
        }
      }
    """

    when:
    run 'assemble'

    then:
    noExceptionThrown()
    file('build/outputs/apk/test-app-debug.apk').exists()
    file('build/intermediates/classes/debug/groovyx/grooid/test/MainActivity.class').exists()
  }
}

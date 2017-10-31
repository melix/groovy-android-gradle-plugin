package groovyx.functional

import groovyx.functional.internal.AndroidFunctionalSpec

class AnnotationProcessingSpec extends AndroidFunctionalSpec {
  def "should compile android app with annotation processing"() {
    given:
    file("settings.gradle") << "rootProject.name = 'test-app'"

    createBuildFileForApplication()
    createAndroidManifest()
    createMainActivityLayoutFile()

    file('src/main/groovy/groovyx/test/MainActivity.groovy') << """
      package groovyx.test

      import android.app.Activity
      import android.os.Bundle
      import groovy.transform.CompileStatic
      import com.google.auto.value.AutoValue

      @CompileStatic
      class MainActivity extends Activity {
        @Override void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState)
          contentView = R.layout.activity_main
          
          def dog = Animal.create 'dog', 4
          println dog
          
          def car = Automobile.create 'car', 4
          println car
        }
      }
      
      @AutoValue
      @CompileStatic
      abstract class Temp {

        static Temp create(String name) {
          new AutoValue_Temp(name)
        }

        abstract String name()
      }
    """

    file('src/main/groovy/groovyx/test/Automobile.java') << """
      package groovyx.test;
      
      import com.google.auto.value.AutoValue;
      
      @AutoValue
      abstract class Automobile {
        static Automobile create(String name, int numberOfWheels) {
          return new AutoValue_Automobile(name, numberOfWheels);
        }
        abstract String name();
        abstract int numberOfWheels();
      }
    """

    file('src/main/java/groovyx/test/Animal.java') << """
      package groovyx.test;
      
      import com.google.auto.value.AutoValue;
      
      @AutoValue
      abstract class Animal {
        static Animal create(String name, int numberOfLegs) {
          return new AutoValue_Animal(name, numberOfLegs);
        }
      
        abstract String name();
        abstract int numberOfLegs();
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

    when:
    copyTestDir()
    run 'assemble', 'test'

    then:
    noExceptionThrown()
    file('build/outputs/apk/test-app-debug.apk').exists()
    file('build/intermediates/classes/debug/groovyx/test/MainActivity.class').exists()

    file('build/intermediates/classes/debug/groovyx/test/Animal.class').exists()
    file('build/generated/source/apt/debug/groovyx/test/AutoValue_Animal.java').exists()
    file('build/generated/source/apt/release/groovyx/test/AutoValue_Animal.java').exists()
    file('build/intermediates/classes/debug/groovyx/test/AutoValue_Animal.class').exists()

    file('build/intermediates/classes/debug/groovyx/test/Automobile.class').exists()
    file('build/generated/source/apt/debug/groovyx/test/AutoValue_Automobile.java').exists()
    file('build/generated/source/apt/release/groovyx/test/AutoValue_Automobile.java').exists()
    file('build/intermediates/classes/debug/groovyx/test/AutoValue_Automobile.class').exists()

    file('build/intermediates/classes/debug/groovyx/test/Temp.class').exists()
    file('build/generated/source/apt/debug/groovyx/test/AutoValue_Temp.java').exists()
    file('build/generated/source/apt/release/groovyx/test/AutoValue_Temp.java').exists()
    file('build/intermediates/classes/debug/groovyx/test/AutoValue_Temp.class').exists()

    file('build/intermediates/classes/androidTest/debug/groovyx/test/AndroidTest.class').exists()
    file('build/intermediates/classes/test/debug/groovyx/test/JvmTest.class').exists()
    file('build/intermediates/classes/test/release/groovyx/test/JvmTest.class').exists()
  }
}

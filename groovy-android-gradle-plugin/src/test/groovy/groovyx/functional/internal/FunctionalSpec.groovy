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

package groovyx.functional.internal

import com.google.common.base.StandardSystemProperty
import groovyx.internal.FileHelper
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

abstract class FunctionalSpec extends Specification implements FileHelper {

  static final String PLUGIN_VERSION = FunctionalSpec.classLoader.getResource('groovyx/groovy-android-gradle-plugin-version.txt').text.trim()
  static final String QUIET_ARGUMENT = '--quiet'

  @Rule TemporaryFolder dir

  GradleRunner runner(String gradleVersion, String... args) {
    return GradleRunner.create()
        .withProjectDir(dir.root)
        .withDebug(true) // always run inline to save memory, especially on CI
        .forwardOutput()
        .withTestKitDir(getTestKitDir())
        .withArguments(args.toList() + QUIET_ARGUMENT)
        .withGradleVersion(gradleVersion?:GradleVersion.current().version)
  }

  BuildResult runWithVersion(String gradleVersion, String... args) {
    runner(gradleVersion, args).build()
  }

  BuildResult run(String... args) {
    runner(null, args).build()
  }

  private static File getTestKitDir() {
    def gradleUserHome = System.getenv('GRADLE_USER_HOME')
    if (!gradleUserHome) {
      gradleUserHome = new File(System.getProperty('user.home'), '.gradle').absolutePath
    }
    return new File(gradleUserHome, 'testkit')
  }

  File getLocalRepo() {
    def rootRelative = new File('build/localrepo')
    return rootRelative.directory ? rootRelative : new File(new File(StandardSystemProperty.USER_DIR.value()).parentFile, 'build/localrepo')
  }
}

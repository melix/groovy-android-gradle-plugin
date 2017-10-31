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

package groovyx.internal

// default values added to be able to run tests in intellij
abstract class TestProperties {
  static boolean isAllTests() {
    return System.getProperty('allTests', 'false') == 'true'
  }

  static String getAndroidPluginVersion() {
    return System.getProperty('androidPluginVersion')?:'3.0.0'
  }

  static String getBuildToolsVersion() {
    return System.getProperty('buildToolsVersion')?:'26.0.2'
  }

  static int getCompileSdkVersion() {
    String prop = System.getProperty('compileSdkVersion')
    return Integer.parseInt(prop?:'25')
  }
}

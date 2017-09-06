/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests.tooling.fixture

import org.gradle.util.GradleVersion


trait ToolingApiUnsupportVersionTrait {
    def minProviderVersion = '1.2'
    def minConsumerVersion = '2.0'
    def minBuildReceiptVersion = GradleVersion.version('1.1')

    def targetVersionMessage(GradleVersion targetVersion) {
        if (targetVersion >= minBuildReceiptVersion) {
            return "You are currently using ${targetVersion.version}. "
        } else {
            return ''
        }
    }

    String unsupportedMessage(GradleVersion targetVersion){
        return "Support for Gradle older than ${minProviderVersion} has been removed. ${targetVersionMessage(targetVersion)}You should upgrade your Gradle to version ${minProviderVersion} or later."
    }
}

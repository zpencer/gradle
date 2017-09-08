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

package org.gradle.integtests.tooling.r43

import org.gradle.integtests.tooling.fixture.TargetGradleVersion
import org.gradle.integtests.tooling.fixture.ToolingApiVersion
import org.gradle.util.GradleVersion

@TargetGradleVersion("current")
class ToolingApiDeprecatedClientCrossVersionSpec extends ToolingApiVersionSupport {
    @ToolingApiVersion(">=2.0 <3.0")
    def "provider give deprecation warning to build request from >=2.0 <3.0 tooling API"() {
        expect:
        buildViaScript().count(clientDeprecationMessage(GradleVersion.current().version)) == 1
    }

    @ToolingApiVersion(">=2.0 <3.0")
    def "provider give deprecation warning to model request from >=2.0 <3.0 tooling API client"() {
        expect:
        getModelViaScript().count(clientDeprecationMessage(GradleVersion.current().version)) == 1
    }

    @ToolingApiVersion(">=2.0 <3.0")
    def "provider give deprecation warning to build action request from >=2.0 <3.0 tooling API client"() {
        expect:
        buildActionViaScript().count(clientDeprecationMessage(GradleVersion.current().version)) == 1
    }

    @ToolingApiVersion(">=2.6 <3.0")
    def "provider give deprecation warning to test execution request from >=2.6 <3.0 tooling API client"() {
        expect:
        testExecutionViaScript().count(clientDeprecationMessage(GradleVersion.current().version)) == 1
    }
}

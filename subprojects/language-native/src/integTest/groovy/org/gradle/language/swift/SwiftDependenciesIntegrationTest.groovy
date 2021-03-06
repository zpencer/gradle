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

package org.gradle.language.swift

import org.gradle.nativeplatform.fixtures.AbstractInstalledToolChainIntegrationSpec
import org.gradle.nativeplatform.fixtures.app.SwiftAppWithLibraries
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition
import org.gradle.vcs.fixtures.GitRepository

@Requires(TestPrecondition.SWIFT_SUPPORT)
class SwiftDependenciesIntegrationTest extends AbstractInstalledToolChainIntegrationSpec {
    def app = new SwiftAppWithLibraries()

    def "can combine swift builds in a composite"() {
        given:
        settingsFile << """
            include 'app'
            includeBuild 'hello'
            includeBuild 'log'
        """

        writeApp()
        writeHelloLibrary()
        writeLogLibrary()

        when:
        succeeds ":app:installDebug"
        then:
        assertTasksExecutedFor("Debug")
        assertAppHasOutputFor("debug")

        when:
        succeeds ":app:installRelease"
        then:
        assertTasksExecutedFor("Release")
        assertAppHasOutputFor("release")
    }

    def "can depend on swift libraries from VCS"() {
        given:
        settingsFile << """
            include 'app'

            sourceControl {
                vcsMappings {
                    addRule("org.gradle.swift VCS rule") { details ->
                        if (details.requested.group == "org.gradle.swift") {
                            from vcs(GitVersionControlSpec) {
                                url = file(details.requested.module).toURI()
                            }
                        }
                    }
                }
            }
        """

        writeApp()
        writeHelloLibrary()
        writeLogLibrary()

        when:
        succeeds ":app:installDebug"
        then:
        assertTasksExecutedFor("Debug")
        assertAppHasOutputFor("debug")

        when:
        succeeds ":app:installRelease"
        then:
        assertTasksExecutedFor("Release")
        assertAppHasOutputFor("release")
    }

    private void assertTasksExecutedFor(String buildType) {
        assert result.assertTasksExecuted(":hello:compile${buildType}Swift", ":hello:link${buildType}", ":log:compile${buildType}Swift", ":log:link${buildType}", ":app:compile${buildType}Swift", ":app:link${buildType}", ":app:install${buildType}")
    }

    private void assertAppHasOutputFor(String buildType) {
        assert installation("app/build/install/main/${buildType}").exec().out == app.expectedOutput
    }

    private writeApp() {
        app.executable.writeToProject(file("app"))
        file("app/build.gradle") << """
            apply plugin: 'swift-executable'
            group = 'org.gradle.swift'
            version = '1.0'

            dependencies {
                implementation 'org.gradle.swift:hello:latest.integration'
            }
        """
    }

    private writeHelloLibrary() {
        def libraryPath = file("hello")
        def libraryRepo = GitRepository.init(libraryPath)
        app.library.writeToProject(libraryPath)
        libraryPath.file("build.gradle") << """
            apply plugin: 'swift-library'
            group = 'org.gradle.swift'
            version = '1.0'
        
            dependencies {
                api 'org.gradle.swift:log:latest.integration'
            }
        """
        libraryPath.file("settings.gradle").touch()
        libraryRepo.commit("initial commit", libraryRepo.listFiles())
        libraryRepo.close()
    }

    private writeLogLibrary() {
        def logPath = file("log")
        def logRepo = GitRepository.init(logPath)
        app.logLibrary.writeToProject(logPath)
        logPath.file("build.gradle") << """
            apply plugin: 'swift-library'
            group = 'org.gradle.swift'
            version = '1.0'
        """
        logPath.file("settings.gradle").touch()
        logRepo.commit("initial commit", logRepo.listFiles())
        logRepo.close()
    }
}

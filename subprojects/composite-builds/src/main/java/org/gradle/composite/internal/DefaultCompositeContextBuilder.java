/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.composite.internal;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.component.BuildIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.internal.SettingsInternal;
import org.gradle.api.internal.artifacts.component.DefaultBuildIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.dependencysubstitution.DependencySubstitutionsInternal;
import org.gradle.api.internal.artifacts.ivyservice.projectmodule.LocalComponentRegistry;
import org.gradle.api.internal.composite.CompositeBuildContext;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.project.ProjectRegistry;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logging;
import org.gradle.includedbuild.IncludedBuild;
import org.gradle.initialization.DefaultProjectDescriptor;
import org.gradle.internal.component.local.model.DefaultLocalComponentMetadata;
import org.gradle.internal.component.local.model.DefaultProjectComponentIdentifier;
import org.gradle.internal.composite.CompositeContextBuilder;
import org.gradle.internal.operations.BuildOperationContext;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.gradle.internal.operations.BuildOperationQueue;
import org.gradle.internal.operations.RunnableBuildOperation;
import org.gradle.internal.progress.BuildOperationDescriptor;
import org.gradle.util.Path;

import java.util.Set;

import static org.gradle.internal.component.local.model.DefaultProjectComponentIdentifier.newProjectId;

public class DefaultCompositeContextBuilder implements CompositeContextBuilder {
    private static final org.gradle.api.logging.Logger LOGGER = Logging.getLogger(DefaultCompositeContextBuilder.class);
    private final BuildOperationExecutor buildOperationExecutor;
    private final DefaultIncludedBuilds allIncludedBuilds;
    private final DefaultProjectPathRegistry projectRegistry;
    private final CompositeBuildContext context;

    public DefaultCompositeContextBuilder(BuildOperationExecutor buildOperationExecutor, DefaultIncludedBuilds allIncludedBuilds, DefaultProjectPathRegistry projectRegistry, CompositeBuildContext context) {
        this.buildOperationExecutor = buildOperationExecutor;
        this.allIncludedBuilds = allIncludedBuilds;
        this.projectRegistry = projectRegistry;
        this.context = context;
    }

    @Override
    public void addIncludedBuilds(final SettingsInternal settings, final Iterable<IncludedBuild> includedBuilds) {
        buildOperationExecutor.runAll(new Action<BuildOperationQueue<RegisterSubstitution>>() {
            @Override
            public void execute(BuildOperationQueue<RegisterSubstitution> queue) {
                ProjectRegistry<DefaultProjectDescriptor> settingsProjectRegistry = settings.getProjectRegistry();
                String rootName = settingsProjectRegistry.getRootProject().getName();
                DefaultBuildIdentifier buildIdentifier = new DefaultBuildIdentifier(rootName, true);
                registerProjects(Path.ROOT, buildIdentifier, settingsProjectRegistry.getAllProjects());

                for (IncludedBuild includedBuild : includedBuilds) {
                    queue.add(new RegisterSubstitution(includedBuild));
                }
            }
        });
        context.markReady();
    }

    void registerProjects(Path rootPath, BuildIdentifier buildIdentifier, Set<DefaultProjectDescriptor> allProjects) {
        for (DefaultProjectDescriptor project : allProjects) {
            Path projectIdentityPath = rootPath.append(project.path());
            ProjectComponentIdentifier projectComponentIdentifier = DefaultProjectComponentIdentifier.newProjectId(buildIdentifier, project.getPath());
            projectRegistry.add(projectIdentityPath, projectComponentIdentifier);
        }
    }

    private class RegisterSubstitution implements RunnableBuildOperation {

        private final IncludedBuild includedBuild;

        public RegisterSubstitution(IncludedBuild includedBuild) {
            this.includedBuild = includedBuild;
        }

        @Override
        public void run(BuildOperationContext context) {
            allIncludedBuilds.registerBuild(includedBuild);
            Path rootProjectPath = Path.ROOT.child(includedBuild.getName());
            BuildIdentifier buildIdentifier = new DefaultBuildIdentifier(includedBuild.getName());
            Set<DefaultProjectDescriptor> allProjects = ((IncludedBuildInternal) includedBuild).getLoadedSettings().getProjectRegistry().getAllProjects();
            registerProjects(rootProjectPath, buildIdentifier, allProjects);

            doAddToCompositeContext((IncludedBuildInternal) includedBuild);
        }

        @Override
        public BuildOperationDescriptor.Builder description() {
            return BuildOperationDescriptor.displayName("Discover substitutions for " + includedBuild.getName());
        }

        private void doAddToCompositeContext(IncludedBuildInternal build) {
            DependencySubstitutionsInternal substitutions = build.resolveDependencySubstitutions();
            if (!substitutions.hasRules()) {
                // Configure the included build to discover substitutions
                LOGGER.info("[composite-build] Configuring build: " + build.getProjectDir());
                Gradle gradle = build.getConfiguredBuild();
                for (Project project : gradle.getRootProject().getAllprojects()) {
                    LocalComponentRegistry localComponentRegistry = ((ProjectInternal) project).getServices().get(LocalComponentRegistry.class);
                    ProjectComponentIdentifier originalIdentifier = newProjectId(project);
                    DefaultLocalComponentMetadata originalComponent = (DefaultLocalComponentMetadata) localComponentRegistry.getComponent(originalIdentifier);
                    ProjectComponentIdentifier componentIdentifier = newProjectId(build, project.getPath());
                    context.registerSubstitution(originalComponent.getId(), componentIdentifier);
                }
            } else {
                // Register the defined substitutions for included build
                context.registerSubstitution(substitutions.getRuleAction());
            }
        }
    }
}

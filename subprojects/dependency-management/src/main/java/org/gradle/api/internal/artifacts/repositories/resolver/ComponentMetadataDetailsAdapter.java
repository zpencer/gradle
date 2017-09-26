/*
 * Copyright 2013 the original author or authors.
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
package org.gradle.api.internal.artifacts.repositories.resolver;

import com.google.common.collect.ImmutableList;
import org.gradle.api.Action;
import org.gradle.api.artifacts.ComponentMetadataDetails;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.moduleconverter.dependencies.DependencyDescriptorFactory;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.internal.component.external.model.AbstractMutableModuleComponentResolveMetadata;
import org.gradle.internal.component.model.DependencyMetadata;
import org.gradle.internal.typeconversion.NotationParser;

import javax.annotation.Nullable;
import java.util.List;

public class ComponentMetadataDetailsAdapter implements ComponentMetadataDetails {
    private final AbstractMutableModuleComponentResolveMetadata metadata;
    private final NotationParser<Object, ExternalModuleDependency> dependencyNotationParser;
    private final DependencyDescriptorFactory dependencyDescriptorFactory;

    public ComponentMetadataDetailsAdapter(AbstractMutableModuleComponentResolveMetadata metadata, NotationParser<Object, ExternalModuleDependency> dependencyNotationParser, DependencyDescriptorFactory dependencyDescriptorFactory) {
        this.metadata = metadata;
        this.dependencyNotationParser = dependencyNotationParser;
        this.dependencyDescriptorFactory = dependencyDescriptorFactory;
    }

    @Override
    public ModuleVersionIdentifier getId() {
        return metadata.getId();
    }

    @Override
    public boolean isChanging() {
        return metadata.isChanging();
    }

    @Override
    public String getStatus() {
        return metadata.getStatus();
    }

    @Override
    public List<String> getStatusScheme() {
        return metadata.getStatusScheme();
    }

    @Override
    public List<ModuleVersionIdentifier> getDependencies() {
        ImmutableList.Builder<ModuleVersionIdentifier> builder = ImmutableList.builder();
        for (DependencyMetadata dependency : metadata.getDependencies()) {
            ModuleVersionSelector requested = dependency.getRequested();
            builder.add(new DefaultModuleVersionIdentifier(requested.getGroup(), requested.getName(), requested.getVersion()));
        }
        return builder.build();
    }

    @Override
    public void setChanging(boolean changing) {
        metadata.setChanging(changing);
    }

    @Override
    public void setStatus(String status) {
        metadata.setStatus(status);
    }

    @Override
    public void setStatusScheme(List<String> statusScheme) {
        metadata.setStatusScheme(statusScheme);
    }

    @Override
    public void removeDependency(Object dependencyNotation) {
        doRemoveDependencies(dependencyNotationParser.parseNotation(dependencyNotation));
    }

    @Override
    public void addDependency(Object dependencyNotation) {
        doAddDependency(dependencyNotationParser.parseNotation(dependencyNotation), null);
    }

    @Override
    public void addDependency(Object dependencyNotation, @Nullable final Action<? super ExternalModuleDependency> configureAction) {
        doAddDependency(dependencyNotationParser.parseNotation(dependencyNotation), configureAction);
    }

    @Override
    public void updateDependency(Object dependencyNotation, @Nullable final Action<? super ExternalModuleDependency> configureAction) {
        doUpdateDependency(dependencyNotationParser.parseNotation(dependencyNotation), configureAction);
    }

    private void doRemoveDependencies(ExternalModuleDependency dependency) {
        DependencyMetadata dependencyMetadata = findDependency(dependency);
        if (dependencyMetadata != null) {
            // This ignores dependency.getModuleConfigurations() for now and removes the dependency from all configurations
            metadata.getDependencies().remove(dependencyMetadata);
        }
    }

    private void doAddDependency(ExternalModuleDependency dependency, @Nullable final Action<? super ExternalModuleDependency> configureAction) {
        if (configureAction != null) {
            configureAction.execute(dependency);
        }
        // We use the default configuration and an empty attributes set. This needs to be adjusted to support variants.
        DependencyMetadata dependencyMetadata = dependencyDescriptorFactory.createDependencyDescriptor(Dependency.DEFAULT_CONFIGURATION, ImmutableAttributes.EMPTY, dependency);
        metadata.getDependencies().add(dependencyMetadata);
    }

    private void doUpdateDependency(ExternalModuleDependency dependency, final Action<? super ExternalModuleDependency> configureAction) {
        DependencyMetadata dependencyMetadata = findDependency(dependency);
        if (dependencyMetadata != null) {
            metadata.getDependencies().remove(dependencyMetadata);
            //make sure the version retained in case the component was selected without version
            dependency.setVersion(dependencyMetadata.getRequested().getVersion());
            doAddDependency(dependency, configureAction);
        }
    }

    private DependencyMetadata findDependency(ExternalModuleDependency dependency) {
        for (DependencyMetadata dependencyMetadata : metadata.getDependencies()) {
            ModuleVersionSelector requested = dependencyMetadata.getRequested();
            if (nullOrEquals(dependency.getGroup(), requested.getGroup()) && nullOrEquals(dependency.getName(), requested.getName()) && nullOrEquals(dependency.getVersion(), requested.getVersion())) {
                return dependencyMetadata;
            }
        }
        return null;
    }

    private boolean nullOrEquals(String toMatch, String other) {
        return toMatch == null || toMatch.equals(other);
    }
}

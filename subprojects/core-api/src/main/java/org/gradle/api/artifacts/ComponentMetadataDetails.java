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
package org.gradle.api.artifacts;

import groovy.lang.Closure;
import org.gradle.api.Incubating;
import org.gradle.api.NonExtensible;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Describes a resolved component's metadata, which typically originates from
 * a component descriptor (Ivy file, Maven POM). Some parts of the metadata can be changed
 * via metadata rules (see {@link org.gradle.api.artifacts.dsl.ComponentMetadataHandler}.
 *
 * @since 1.8
 */
@Incubating
@NonExtensible
public interface ComponentMetadataDetails extends ComponentMetadata {
    /**
     * Sets whether the component is changing or immutable.
     *
     * @param changing whether the component is changing or immutable
     */
    void setChanging(boolean changing);

    /**
     * Sets the status of the component. Must
     * match one of the values in {@link #getStatusScheme()}.
     *
     * @param status the status of the component
     */
    void setStatus(String status);

    /**
     * Sets the status scheme of the component. Values are ordered
     * from least to most mature status.
     *
     * @param statusScheme the status scheme of the component
     */
    void setStatusScheme(List<String> statusScheme);

    /**
     * Removes all dependencies from the component. This is useful to resolve version conflicts
     * or to replace dependencies with equivalent implementations which are then added
     * via {@link #addDependency(String, Object)}.
     *
     * @param configurationName the configuration for the dependency
     * @since 4.3
     */
    void removeAllDependencies(String configurationName);

    /**
     * Removes dependencies from the component which belong to the given group.
     *
     * @param configurationName the configuration for the dependency
     * @param group the group (e.g. 'org.example')
     * @since 4.3
     */
    void removeDependencies(String configurationName, String group);

    /**
     * Remove one dependency from the component.
     *
     * @param configurationName the configuration for the dependency
     * @param dependencyNotation the dependency to remove (e.g. 'org.example:module1')
     * @since 4.3
     */
    void removeDependency(String configurationName, Object dependencyNotation);

    /**
     * Add an additional dependency to the component. This can be used to add replacements for
     * dependencies removed through one of the 'removeDependency' methods.
     *
     * @param configurationName the configuration for the dependency
     * @param dependencyNotation the dependency (e.g. 'org.example:module2')
     * @since 4.3
     */
    void addDependency(String configurationName, Object dependencyNotation);

    /**
     * Add an additional dependency to the component. This can be used to add replacements for
     * dependencies removed through one of the 'removeDependency' methods.
     *
     * @param configurationName the configuration for the dependency
     * @param dependencyNotation the dependency (e.g. 'org.example:module2')
     * @param configureClosure further configuration of the dependency (of type {@link ModuleDependency}).
     * @since 4.3
     */
    void addDependency(String configurationName, Object dependencyNotation, @Nullable Closure configureClosure);
}

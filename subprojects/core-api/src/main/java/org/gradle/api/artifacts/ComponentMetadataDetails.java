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

import org.gradle.api.Action;
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
     * Remove a dependency from the component.
     *
     * @param dependencyNotation the dependency to remove (e.g. 'org.example:module1')
     * @since 4.4
     */
    void removeDependency(Object dependencyNotation);

    /**
     * Add an additional dependency to the component. This can be used to add replacements for
     * dependencies removed through one of the 'removeDependency' methods.
     *
     * @param dependencyNotation the dependency (e.g. 'org.example:module2')
     * @since 4.4
     */
    void addDependency(Object dependencyNotation);

    /**
     * Add an additional dependency to the component. This can be used to add replacements for
     * dependencies removed through one of the 'removeDependency' methods.
     *
     * @param dependencyNotation the dependency (e.g. 'org.example:module2')
     * @param configureAction further configuration of the dependency (of type {@link ExternalModuleDependency}).
     * @since 4.4
     */
    void addDependency(Object dependencyNotation, @Nullable Action<? super ExternalModuleDependency> configureAction);

    /**
     * Change properties (e.g. the version) of an existing dependency
     *
     * @param dependencyNotation the dependency (e.g. 'org.example:module2')
     * @param configureAction configuration of the dependency (of type {@link ExternalModuleDependency}).
     * @since 4.4
     */
    void updateDependency(Object dependencyNotation, final Action<? super ExternalModuleDependency> configureAction);
}

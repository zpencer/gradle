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

package org.gradle.internal.composite;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.gradle.StartParameter;
import org.gradle.api.GradleException;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.SettingsInternal;
import org.gradle.includedbuild.IncludedBuild;
import org.gradle.includedbuild.internal.IncludedBuildFactory;
import org.gradle.initialization.SettingsLoader;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class CompositeBuildSettingsLoader implements SettingsLoader {
    private final SettingsLoader delegate;
    private final IncludedBuildFactory includedBuildFactory;

    public CompositeBuildSettingsLoader(SettingsLoader delegate, IncludedBuildFactory includedBuildFactory) {
        this.delegate = delegate;
        this.includedBuildFactory = includedBuildFactory;
    }

    @Override
    public SettingsInternal findAndLoadSettings(GradleInternal gradle) {
        SettingsInternal settings = delegate.findAndLoadSettings(gradle);

        Collection<IncludedBuild> includedBuilds = getIncludedBuilds(gradle.getStartParameter(), settings);
        if (!includedBuilds.isEmpty()) {
            if (gradle.getStartParameter().getMaxWorkerCount() <= includedBuilds.size()) {
                throw new GradleException("Use --max-workers to provide at least one worker per build in a composite");
            }
            gradle.setIncludedBuilds(includedBuilds);
        }

        return settings;
    }

    private Collection<IncludedBuild> getIncludedBuilds(StartParameter startParameter, SettingsInternal settings) {
        Map<File, IncludedBuild> includedBuildMap = Maps.newLinkedHashMap();
        includedBuildMap.putAll(settings.getIncludedBuilds());

        for (File file : startParameter.getIncludedBuilds()) {
            if (!includedBuildMap.containsKey(file)) {
                includedBuildMap.put(file, includedBuildFactory.createBuild(file));
            }
        }

        return validateBuildNames(includedBuildMap.values(), settings);
    }

    private Collection<IncludedBuild> validateBuildNames(Collection<IncludedBuild> builds, SettingsInternal settings) {
        Set<String> names = Sets.newHashSet();
        for (IncludedBuild build : builds) {
            String buildName = build.getName();
            if (!names.add(buildName)) {
                throw new GradleException("Included build '" + buildName + "' is not unique in composite.");
            }
            if (settings.getRootProject().getName().equals(buildName)) {
                throw new GradleException("Included build '" + buildName + "' collides with root project name.");
            }
            if (settings.findProject(":" + buildName) != null) {
                throw new GradleException("Included build '" + buildName + "' collides with subproject of the same name.");
            }
        }
        return builds;
    }

}

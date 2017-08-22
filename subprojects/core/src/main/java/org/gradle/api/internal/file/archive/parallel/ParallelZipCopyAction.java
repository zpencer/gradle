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

package org.gradle.api.internal.file.archive.parallel;

import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.internal.tasks.SimpleWorkResult;
import org.gradle.api.tasks.WorkResult;
import org.gradle.internal.operations.BuildOperationExecutor;

import java.io.File;

public class ParallelZipCopyAction implements CopyAction {
    private final File zipFile;
    private final boolean isZip64;
    private final String encoding;
    private final boolean preserveTimestamps;
    private final BuildOperationExecutor buildOperationExecutor;

    public ParallelZipCopyAction(File zipFile,
                                 boolean isZip64,
                                 String encoding,
                                 boolean preserveFileTimestamps,
                                 BuildOperationExecutor buildOperationExecutor) {
        this.zipFile = zipFile;
        this.isZip64 = isZip64;
        this.encoding = encoding;
        this.preserveTimestamps = preserveFileTimestamps;
        this.buildOperationExecutor = buildOperationExecutor;
    }

    @Override
    public WorkResult execute(CopyActionProcessingStream stream) {
        final ParallelZipCreator creator = new ParallelZipCreator(buildOperationExecutor, zipFile, isZip64, preserveTimestamps, encoding);
        creator.createZip(stream);
        return new SimpleWorkResult(true);
    }
}

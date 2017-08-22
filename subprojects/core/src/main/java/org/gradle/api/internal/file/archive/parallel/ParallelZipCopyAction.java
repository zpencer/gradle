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

import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.file.archive.ZipCopyAction;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.gradle.api.internal.tasks.SimpleWorkResult;
import org.gradle.api.tasks.WorkResult;
import org.gradle.internal.UncheckedException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;

public class ParallelZipCopyAction implements CopyAction {
    private final File zipFile;
    private final boolean isZip64;
    private final String encoding;
    private final boolean preserveTimestamps;

    public ParallelZipCopyAction(File zipFile, boolean isZip64, String encoding, boolean preserveFileTimestamps) {
        this.zipFile = zipFile;
        this.isZip64 = isZip64;
        this.encoding = encoding;
        this.preserveTimestamps = preserveFileTimestamps;
    }

    @Override
    public WorkResult execute(CopyActionProcessingStream stream) {
        final ParallelScatterZipCreator creator = new ParallelScatterZipCreator();
        try {
            final ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(zipFile);
            zipOutput.setEncoding(encoding);
            zipOutput.setUseZip64(isZip64 ? Zip64Mode.Always : Zip64Mode.AsNeeded);
            stream.process(new ProcessEntry(creator, zipOutput, preserveTimestamps));
            creator.writeTo(zipOutput);
            zipOutput.close();
        } catch (IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        } catch (InterruptedException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        } catch (ExecutionException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
        return new SimpleWorkResult(true);
    }

    private static class ProcessEntry implements CopyActionProcessingStreamAction {
        private final ParallelScatterZipCreator creator;
        private final ZipArchiveOutputStream zipOutput;
        private final boolean preserveFileTimestamps;

        public ProcessEntry(ParallelScatterZipCreator creator, ZipArchiveOutputStream zipOutput, boolean preserveFileTimestamps) {
            this.creator = creator;
            this.zipOutput = zipOutput;
            this.preserveFileTimestamps = preserveFileTimestamps;
        }

        public void processFile(FileCopyDetailsInternal details) {
            if (details.isDirectory()) {
                visitDir(details);
            } else {
                visitFile(details);
            }
        }

        private void visitFile(final FileCopyDetails details) {
            ZipArchiveEntry entry = new ZipArchiveEntry(details.getRelativePath().getPathString());
            entry.setSize(details.getSize());

            entry.setMethod(ZipEntry.DEFLATED);
            creator.addArchiveEntry(entry, new InputStreamSupplier() {
                @Override
                public InputStream get() {
                    return details.open();
                }
            });
        }

        private void visitDir(FileCopyDetails dirDetails) {
            ZipArchiveEntry archiveEntry = new ZipArchiveEntry(dirDetails.getRelativePath().getPathString() + '/');
            archiveEntry.setTime(getArchiveTimeFor(dirDetails));
            archiveEntry.setUnixMode(UnixStat.DIR_FLAG | dirDetails.getMode());
            archiveEntry.setMethod(ZipEntry.STORED);
            try {
                zipOutput.putArchiveEntry(archiveEntry);
                zipOutput.closeArchiveEntry();
            } catch (IOException e) {
                throw UncheckedException.throwAsUncheckedException(e);
            }

        }

        private long getArchiveTimeFor(FileCopyDetails details) {
            return preserveFileTimestamps ? details.getLastModified() : ZipCopyAction.CONSTANT_TIME_FOR_ZIP_ENTRIES;
        }
    }
}

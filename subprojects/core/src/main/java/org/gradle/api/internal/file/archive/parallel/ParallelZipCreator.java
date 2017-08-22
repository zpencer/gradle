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

import org.apache.commons.compress.archivers.zip.ScatterZipOutputStream;
import org.apache.commons.compress.archivers.zip.StreamCompressor;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntryRequest;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.parallel.FileBasedScatterGatherBackingStore;
import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.apache.commons.compress.parallel.ScatterGatherBackingStore;
import org.apache.commons.compress.parallel.ScatterGatherBackingStoreSupplier;
import org.gradle.api.Action;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.file.archive.ZipCopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.operations.BuildOperationContext;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.gradle.internal.operations.BuildOperationQueue;
import org.gradle.internal.operations.RunnableBuildOperation;
import org.gradle.internal.progress.BuildOperationDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;

public class ParallelZipCreator {
    private final BuildOperationExecutor executor;
    private final File zipFile;
    private final boolean isZip64;
    private final boolean isPreserveTimestamps;
    private final String encoding;
    private final ScatterGatherBackingStoreSupplier supplier;

    public ParallelZipCreator(BuildOperationExecutor executor, File zipFile, boolean isZip64, boolean isPreserveTimestamps, String encoding) {
        this.executor = executor;
        this.zipFile = zipFile;
        this.isZip64 = isZip64;
        this.isPreserveTimestamps = isPreserveTimestamps;
        this.encoding = encoding;
        this.supplier = new DefaultBackingStoreSupplier();
    }

    void createZip(final CopyActionProcessingStream stream) {
        try {
            final ConcurrentLinkedDeque<ScatterZipOutputStream> streams = new ConcurrentLinkedDeque<ScatterZipOutputStream>();
            final ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(zipFile);
            executor.runAll(new Action<BuildOperationQueue<RunnableBuildOperation>>() {
                @Override
                public void execute(BuildOperationQueue<RunnableBuildOperation> buildOperationQueue) {
                    zipOutput.setEncoding(encoding);
                    zipOutput.setUseZip64(isZip64 ? Zip64Mode.Always : Zip64Mode.AsNeeded);
                    stream.process(new ProcessEntry(buildOperationQueue, zipOutput, streams, isPreserveTimestamps, supplier));
                }
            });
            for (final ScatterZipOutputStream scatterStream : streams) {
                scatterStream.writeTo(zipOutput);
                scatterStream.close();
            }
            zipOutput.close();
        } catch (IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

    private static class ProcessEntry implements CopyActionProcessingStreamAction {
        private final BuildOperationQueue<RunnableBuildOperation> buildOperationQueue;
        private final ZipArchiveOutputStream zipOutput;
        private final ConcurrentLinkedDeque<ScatterZipOutputStream> streams;
        private final boolean preserveFileTimestamps;
        private final ScatterGatherBackingStoreSupplier backingStoreSupplier;

        public ProcessEntry(BuildOperationQueue<RunnableBuildOperation> buildOperationQueue,
                            ZipArchiveOutputStream zipOutput,
                            ConcurrentLinkedDeque<ScatterZipOutputStream> streams,
                            boolean preserveFileTimestamps,
                            ScatterGatherBackingStoreSupplier backingStoreSupplier) {
            this.buildOperationQueue = buildOperationQueue;
            this.zipOutput = zipOutput;
            this.streams = streams;
            this.preserveFileTimestamps = preserveFileTimestamps;
            this.backingStoreSupplier = backingStoreSupplier;
        }

        public void processFile(FileCopyDetailsInternal details) {
            if (details.isDirectory()) {
                visitDir(details);
            } else {
                visitFile(details);
            }
        }

        private void visitFile(final FileCopyDetails details) {
            final String pathString = details.getRelativePath().getPathString();
            final ZipArchiveEntry entry = new ZipArchiveEntry(pathString);
            entry.setSize(details.getSize());
            entry.setMethod(ZipEntry.DEFLATED);
            buildOperationQueue.add(new RunnableBuildOperation() {
                @Override
                public void run(BuildOperationContext context) {
                    ScatterZipOutputStream stream = scatterZipOutputStream();
                    try {
                        stream.addArchiveEntry(ZipArchiveEntryRequest.createZipArchiveEntryRequest(entry, new InputStreamSupplier() {
                            @Override
                            public InputStream get() {
                                return details.open();
                            }
                        }));
                    } catch (IOException e) {
                        throw UncheckedException.throwAsUncheckedException(e);
                    } finally {
                        streams.add(stream);
                    }
                }

                @Override
                public BuildOperationDescriptor.Builder description() {
                    return BuildOperationDescriptor.displayName("Compressing " + pathString).progressDisplayName("Compressing " + pathString);
                }
            });
        }

        protected ScatterZipOutputStream scatterZipOutputStream() {
            ScatterZipOutputStream stream = streams.pollFirst();
            if (stream == null) {
                ScatterGatherBackingStore backingStore;
                try {
                    backingStore = backingStoreSupplier.get();
                } catch (IOException e) {
                    throw UncheckedException.throwAsUncheckedException(e);
                }
                stream = new ScatterZipOutputStream(backingStore, StreamCompressor.create(Deflater.DEFAULT_COMPRESSION, backingStore));
            }
            return stream;
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

    private static class DefaultBackingStoreSupplier implements ScatterGatherBackingStoreSupplier {
        private final static AtomicInteger STORE_NUM = new AtomicInteger(0);

        @Override
        public ScatterGatherBackingStore get() throws IOException {
            final File tempFile = File.createTempFile("parallelscatter", "n" + STORE_NUM.incrementAndGet());
            return new FileBasedScatterGatherBackingStore(tempFile);
        }
    }
}

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

package org.gradle.caching.internal.tasks;

import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.parallel.InputStreamSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;

public class CommonsZipPacker implements Packer {

    private final byte[] buffer;
    private final boolean parallel;

    public CommonsZipPacker(int bufferSizeInKBytes, boolean parallel) {
        this.buffer = new byte[bufferSizeInKBytes];
        this.parallel = parallel;
    }

    @Override
    public void pack(List<DataSource> inputs, DataTarget output) throws IOException {
        if (parallel) {
            parallelPack(inputs, output);
        } else {
            serialPack(inputs, output);
        }
    }

    private void serialPack(List<DataSource> inputs, DataTarget output) throws IOException {
        ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(output.openOutput());
        for (DataSource input : inputs) {
            ZipArchiveEntry entry = new ZipArchiveEntry(input.getName());
            entry.setSize(input.getLength());
            zipOutput.putArchiveEntry(entry);
            PackerUtils.packEntry(input, zipOutput, buffer);
            zipOutput.closeArchiveEntry();
        }
        zipOutput.close();
    }

    private void parallelPack(List<DataSource> inputs, DataTarget output) throws IOException {
        ParallelScatterZipCreator creator = new ParallelScatterZipCreator();
        ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(output.openOutput());
        for (final DataSource input : inputs) {
            ZipArchiveEntry entry = new ZipArchiveEntry(input.getName());
            entry.setSize(input.getLength());
            entry.setMethod(ZipEntry.DEFLATED);
            creator.addArchiveEntry(entry, new InputStreamSupplier() {
                @Override
                public InputStream get() {
                    try {
                        return input.openInput();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        try {
            creator.writeTo(zipOutput);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        zipOutput.close();
    }


    @Override
    public void unpack(DataSource input, DataTargetFactory targetFactory) throws IOException {
        ZipArchiveInputStream zipInput = new ZipArchiveInputStream(input.openInput());
        while (true) {
            ZipArchiveEntry entry = zipInput.getNextZipEntry();
            if (entry == null) {
                break;
            }
            PackerUtils.unpackEntry(entry.getName(), zipInput, buffer, targetFactory);
        }
        zipInput.close();
    }
}

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
package org.gradle.api.internal.tasks;

import org.apache.commons.io.IOUtils;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.logging.sink.OutputEventRenderer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UserInputHandler {
    private final OutputEventRenderer outputEventRenderer;

    public UserInputHandler(OutputEventRenderer outputEventRenderer) {
        this.outputEventRenderer = outputEventRenderer;
    }

    public String getUserResponse(String prompt) {
        outputEventRenderer.onOutput(new org.gradle.internal.logging.events.UserInputRequestEvent(prompt));
        try {
            return readInput();
        } finally {
            outputEventRenderer.onOutput(new org.gradle.internal.logging.events.UserInputResumeEvent());
        }
    }

    private String readInput() {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        try {
            return bufferedReader.readLine();
        } catch (IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        } finally {
            IOUtils.closeQuietly(bufferedReader);
        }
    }

}

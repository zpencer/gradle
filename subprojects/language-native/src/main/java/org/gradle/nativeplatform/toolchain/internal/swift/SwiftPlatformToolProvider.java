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
package org.gradle.nativeplatform.toolchain.internal.swift;

import org.gradle.language.base.internal.compile.CompileSpec;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.language.base.internal.compile.CompilerUtil;
import org.gradle.nativeplatform.internal.CompilerOutputFileNamingSchemeFactory;
import org.gradle.nativeplatform.internal.LinkerSpec;
import org.gradle.nativeplatform.platform.internal.OperatingSystemInternal;
import org.gradle.nativeplatform.toolchain.SwiftcPlatformToolChain;
import org.gradle.nativeplatform.toolchain.internal.AbstractPlatformToolProvider;
import org.gradle.nativeplatform.toolchain.internal.CommandLineToolContext;
import org.gradle.nativeplatform.toolchain.internal.DefaultMutableCommandLineToolContext;
import org.gradle.nativeplatform.toolchain.internal.MutableCommandLineToolContext;
import org.gradle.nativeplatform.toolchain.internal.NativeCompilerFactory;
import org.gradle.nativeplatform.toolchain.internal.ToolType;
import org.gradle.nativeplatform.toolchain.internal.compilespec.SwiftCompileSpec;
import org.gradle.nativeplatform.toolchain.internal.tools.CommandLineToolConfigurationInternal;
import org.gradle.nativeplatform.toolchain.internal.tools.ToolSearchPath;

import java.io.File;

class SwiftPlatformToolProvider extends AbstractPlatformToolProvider {
    private final ToolSearchPath toolSearchPath;
    private final NativeCompilerFactory compilerFactory;
    private final SwiftcPlatformToolChain toolRegistry;
    private final CompilerOutputFileNamingSchemeFactory compilerOutputFileNamingSchemeFactory;

    SwiftPlatformToolProvider(NativeCompilerFactory compilerFactory, OperatingSystemInternal targetOperatingSystem, ToolSearchPath toolSearchPath, SwiftcPlatformToolChain toolRegistry, CompilerOutputFileNamingSchemeFactory compilerOutputFileNamingSchemeFactory) {
        super(targetOperatingSystem);
        this.compilerFactory = compilerFactory;
        this.toolRegistry = toolRegistry;
        this.toolSearchPath = toolSearchPath;
        this.compilerOutputFileNamingSchemeFactory = compilerOutputFileNamingSchemeFactory;
    }

    @Override
    public <T extends CompileSpec> org.gradle.language.base.internal.compile.Compiler<T> newCompiler(Class<T> spec) {
        if (SwiftCompileSpec.class.isAssignableFrom(spec)) {
            return CompilerUtil.castCompiler(createSwiftCompiler());
        }
        return super.newCompiler(spec);
    }

    @Override
    protected Compiler<LinkerSpec> createLinker() {
        CommandLineToolConfigurationInternal linkerTool = (CommandLineToolConfigurationInternal) toolRegistry.getLinker();
        SwiftLinker linker = new SwiftLinker(context(ToolType.LINKER, "swiftc", linkerTool));
        return compilerFactory.compiler(linker);
    }

    private Compiler<SwiftCompileSpec> createSwiftCompiler() {
        CommandLineToolConfigurationInternal swiftCompilerTool = (CommandLineToolConfigurationInternal) toolRegistry.getSwiftCompiler();
        SwiftCompiler compiler = new SwiftCompiler(compilerOutputFileNamingSchemeFactory, context(ToolType.SWIFT_COMPILER, "swiftc", swiftCompilerTool), getObjectFileExtension());
        return compilerFactory.compiler(compiler);
    }

    private CommandLineToolContext context(ToolType key, String exeName, CommandLineToolConfigurationInternal toolConfiguration) {
        File toolExe = toolSearchPath.locate(key, exeName).getTool();
        MutableCommandLineToolContext baseInvocation = new DefaultMutableCommandLineToolContext(key.getToolName(), toolExe);
        baseInvocation.setArgAction(toolConfiguration.getArgAction());
        return baseInvocation;
    }
}

/*
 * This file is licensed under the MIT License, part of architectury-loom-runtime.
 * Copyright (c) 2021-2023 architectury
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.architectury.mixinremapperservice;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class ArchitecturyMixinRemapperInjectorServiceImpl {
	private static final Logger LOGGER = LogManager.getLogger("ArchitecturyRemapperInjector");

	private static final String MAPPINGS_PATH_PROPERTY = "architectury.mixinRemapper.mappingsPath";
	private static final String SOURCE_NAMESPACE_PROPERTY = "architectury.mixinRemapper.sourceNamespace";

	public static void attach() {
		LOGGER.debug("We will be injecting our remapper.");

		try {
			String sourceNamespace = System.getProperty(SOURCE_NAMESPACE_PROPERTY);
			MixinEnvironment.getDefaultEnvironment().getRemappers().add(new MixinIntermediaryDevRemapper(Objects.requireNonNull(resolveMappings()), sourceNamespace, "named"));
			LOGGER.debug("We have successfully injected our remapper.");
		} catch (Exception e) {
			LOGGER.debug("We have failed to inject our remapper.", e);
		}
	}

	private static MappingTree resolveMappings() {
		try {
			String mappingsPathProperty = System.getProperty(MAPPINGS_PATH_PROPERTY);
			Path path = Paths.get(mappingsPathProperty);

			MemoryMappingTree mappingTree = new MemoryMappingTree();
			MappingReader.read(path, mappingTree);
			return mappingTree;
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			return null;
		}
	}
}

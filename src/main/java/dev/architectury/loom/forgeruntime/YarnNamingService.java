/*
 * This file is licensed under the MIT License, part of architectury-loom-runtime.
 * Copyright (c) 2020, 2021 architectury
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

package dev.architectury.loom.forgeruntime;

import cpw.mods.modlauncher.api.INameMappingService;
import net.fabricmc.mapping.tree.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class YarnNamingService implements INameMappingService {
	// Namespaces in mapping file
	private static final String SOURCE_NAMESPACE = "srg";
	private static final String TARGET_NAMESPACE = "named";

	private static final String PATH_TO_MAPPINGS = "fabric.yarnWithSrg.path";

	private Map<String, String> classNameMappings = null;
	private Map<String, String> methodNameMappings = null;
	private Map<String, String> fieldNameMappings = null;

	@Override
	public String mappingName() {
		return "srgtoyarn";
	}

	@Override
	public String mappingVersion() {
		return "1";
	}

	@Override
	public Map.Entry<String, String> understanding() {
		return new AbstractMap.SimpleImmutableEntry<>("srg", "mcp");
	}

	@Override
	public BiFunction<Domain, String, String> namingFunction() {
		return this::remap;
	}

	private void generateMappings() {
		if (classNameMappings != null) {
			return;
		}

		String pathStr = System.getProperty(PATH_TO_MAPPINGS);
		if (pathStr == null) throw new RuntimeException("Missing system property '" + PATH_TO_MAPPINGS + "'!");
		Path path = Paths.get(pathStr);

		TinyTree mappings;
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			mappings = TinyMappingFactory.loadWithDetection(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		classNameMappings = new HashMap<>();
		fieldNameMappings = new HashMap<>();
		methodNameMappings = new HashMap<>();
		buildNameMap(mappings.getClasses(), classNameMappings, clz -> {
			buildNameMap(clz.getMethods(), methodNameMappings, null);
			buildNameMap(clz.getFields(), fieldNameMappings, null);
		});
	}

	private <M extends Mapped> void buildNameMap(Collection<M> entries, Map<String, String> target, Consumer<M> entryConsumer) {
		for (M entry : entries) {
			target.put(entry.getName(SOURCE_NAMESPACE), entry.getName(TARGET_NAMESPACE));
			if (entryConsumer != null) {
				entryConsumer.accept(entry);
			}
		}
	}

	private String remap(Domain domain, String name) {
		// ensure the mapping tables are built
		generateMappings();

		switch (domain) {
			case CLASS:
				boolean dot = name.contains(".");
				String searchName = maybeReplace(dot, name, '.', '/');
				String target = classNameMappings.get(searchName);
				return target != null ? maybeReplace(dot, target, '/', '.') : name;
			case METHOD:
				return methodNameMappings.getOrDefault(name, name);
			case FIELD:
				return fieldNameMappings.getOrDefault(name, name);
			default:
				return name;
		}
	}

	private static String maybeReplace(boolean run, String s, char from, char to) {
		return run ? s.replace(from, to) : s;
	}
}

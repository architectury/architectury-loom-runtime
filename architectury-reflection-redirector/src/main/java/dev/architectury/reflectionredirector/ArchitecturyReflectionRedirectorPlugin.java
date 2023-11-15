/*
 * This file is licensed under the MIT License, part of architectury-loom-runtime.
 * Copyright (c) 2020-2023 architectury
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

package dev.architectury.reflectionredirector;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

public class ArchitecturyReflectionRedirectorPlugin implements ILaunchPluginService {
    public static ArchitecturyReflectionRedirectorPlugin instance;
    public static final EnumSet<Phase> AFTER_ONLY = EnumSet.of(Phase.AFTER);
    // Namespaces in mapping file
    private static final String TARGET_NAMESPACE = "named";

    private static final String MAPPINGS_PATH_PROPERTY = "architectury.reflectionredirector.mappingsPath";
    private static final String SOURCE_NAMESPACE_PROPERTY = "architectury.reflectionredirector.sourceNamespace";

    private String sourceNamespace;
    private MappingTree mappings;

    public ArchitecturyReflectionRedirectorPlugin() {
        instance = this;
    }

    public static ArchitecturyReflectionRedirectorPlugin getInstance() {
        return instance;
    }

    @Override
    public String name() {
        return "architectury-reflection-redirector";
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        return ArchitecturyReflectionRedirectorPlugin.AFTER_ONLY;
    }

    @Override
    public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
        ReflectionRedirector.redirect(classNode);
        return true; // We don't need to rewrite frames
    }

    @Override
    public int processClassWithFlags(Phase phase, ClassNode classNode, Type classType, String reason) {
        processClass(phase, classNode, classType);
        return ComputeFlags.SIMPLE_REWRITE;
    }

    private void generateMappings() {
        if (mappings != null) {
            return;
        }

        sourceNamespace = getRequiredProperty(SOURCE_NAMESPACE_PROPERTY);
        Path path = Paths.get(getRequiredProperty(MAPPINGS_PATH_PROPERTY));

        MemoryMappingTree mappings = new MemoryMappingTree(true);
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            MappingReader.read(reader, mappings);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.mappings = mappings;
    }

    private static String getRequiredProperty(String property) {
        final String value = System.getProperty(property);
        if (value == null) throw new RuntimeException("Missing required system property '" + property + "'!");
        return value;
    }

    public String remapClass(String name) {
        // ensure the mapping tables are built
        generateMappings();

        boolean dot = name.contains(".");
        String searchName = maybeReplace(dot, name, '.', '/');
        MappingTree.ClassMapping clazz = this.mappings.getClass(searchName, this.mappings.getNamespaceId(sourceNamespace));
        String target = clazz != null ? clazz.getName(TARGET_NAMESPACE) : null;
        return target != null ? maybeReplace(dot, target, '/', '.') : name;
    }

    public String remapMethod(String owner, String name, String desc) {
        // ensure the mapping tables are built
        generateMappings();

        String searchName = maybeReplace(owner.contains("."), owner, '.', '/');
        MappingTree.ClassMapping clazz = this.mappings.getClass(searchName, this.mappings.getNamespaceId(TARGET_NAMESPACE));
        if (clazz == null) return name;
        MappingTree.MethodMapping method = clazz.getMethod(name, this.mappings.mapDesc(desc, this.mappings.getNamespaceId(TARGET_NAMESPACE), this.mappings.getNamespaceId(sourceNamespace)), this.mappings.getNamespaceId(sourceNamespace));
        if (method == null) return name;
        String target = method.getName(TARGET_NAMESPACE);
        return target != null ? target : name;
    }

    public String remapField(String owner, String name) {
        // ensure the mapping tables are built
        generateMappings();

        String searchName = maybeReplace(owner.contains("."), owner, '.', '/');
        MappingTree.ClassMapping clazz = this.mappings.getClass(searchName, this.mappings.getNamespaceId(TARGET_NAMESPACE));
        if (clazz == null) return name;
        MappingTree.FieldMapping field = clazz.getField(name, null, this.mappings.getNamespaceId(sourceNamespace));
        if (field == null) return name;
        String target = field.getName(TARGET_NAMESPACE);
        return target != null ? target : name;
    }

    private static String maybeReplace(boolean run, String s, char from, char to) {
        return run ? s.replace(from, to) : s;
    }
}

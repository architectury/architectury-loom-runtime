/*
 * This file is licensed under the MIT License, part of architectury-loom-runtime.
 * Copyright (c) 2023 architectury
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

import net.fabricmc.mappingio.tree.MappingTreeView;
import org.spongepowered.asm.mixin.extensibility.IRemapper;

class MixinRemapper implements IRemapper {
    private final MappingTreeView mappings;
    private final int from;
    private final int to;

    MixinRemapper(MappingTreeView mappings, String from, String to) {
        this.mappings = mappings;
        this.from = mappings.getNamespaceId(from);
        this.to = mappings.getNamespaceId(to);

        if (this.from == MappingTreeView.NULL_NAMESPACE_ID) {
            throw new IllegalArgumentException("Source ns '" + from + "' not present in mapping tree");
        } else if (this.to == MappingTreeView.NULL_NAMESPACE_ID) {
            throw new IllegalArgumentException("Target ns '" + to + "' not present in mapping tree");
        }
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        final MappingTreeView.MethodMappingView method = mappings.getMethod(owner, name, desc, from);
        return method != null ? method.getName(to) : name;
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        final MappingTreeView.FieldMappingView field = mappings.getField(owner, name, desc, from);
        return field != null ? field.getName(to) : name;
    }

    @Override
    public String map(String typeName) {
        return mappings.mapClassName(typeName, from, to);
    }

    @Override
    public String unmap(String typeName) {
        return mappings.mapClassName(typeName, to, from);
    }

    @Override
    public String mapDesc(String desc) {
        return mappings.mapDesc(desc, from, to);
    }

    @Override
    public String unmapDesc(String desc) {
        return mappings.mapDesc(desc, to, from);
    }
}

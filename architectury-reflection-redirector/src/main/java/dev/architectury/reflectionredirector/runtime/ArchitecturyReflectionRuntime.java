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

package dev.architectury.reflectionredirector.runtime;

import dev.architectury.reflectionredirector.ArchitecturyReflectionRedirectorPlugin;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ArchitecturyReflectionRuntime {
    public static Class<?> forName(String name) throws ClassNotFoundException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return forName(name, true, loader);
    }

    public static Class<?> forName(String name, boolean initialize, ClassLoader classLoader) throws ClassNotFoundException {
        String remapped = ArchitecturyReflectionRedirectorPlugin.getInstance().remapClass(name);

        if (remapped.equals(name)) {
            return Class.forName(name, initialize, classLoader);
        } else {
            try {
                return Class.forName(remapped, initialize, classLoader);
            } catch (ClassNotFoundException e) {
                return Class.forName(name, initialize, classLoader);
            }
        }
    }

    public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        // MIO supports parameter only descriptors
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append('(');
        for (Class<?> argumentType : parameterTypes) {
            strBuilder.append(Type.getType(argumentType).getDescriptor());
        }
        strBuilder.append(')');

        String remapped = ArchitecturyReflectionRedirectorPlugin.getInstance().remapMethod(clazz.getName(), name, strBuilder.toString());

        if (remapped.equals(name)) {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } else {
            try {
                return clazz.getDeclaredMethod(remapped, parameterTypes);
            } catch (NoSuchMethodException e) {
                return clazz.getDeclaredMethod(name, parameterTypes);
            }
        }
    }

    public static Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        // Try to walk up the class hierarchy to find the method, also interfaces
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            for (Class<?> anInterface : currentClass.getInterfaces()) {
                try {
                    return getMethod(anInterface, name, parameterTypes);
                } catch (NoSuchMethodException ignored) {
                }
            }
            try {
                return getDeclaredMethod(currentClass, name, parameterTypes);
            } catch (NoSuchMethodException e) {
                currentClass = currentClass.getSuperclass();
            }
        }

        throw new NoSuchMethodException();
    }

    public static Field getDeclaredField(Class<?> clazz, String name) throws NoSuchFieldException {
        String remapped = ArchitecturyReflectionRedirectorPlugin.getInstance().remapField(clazz.getName(), name);

        if (remapped.equals(name)) {
            return clazz.getDeclaredField(name);
        } else {
            try {
                return clazz.getDeclaredField(remapped);
            } catch (NoSuchFieldException e) {
                return clazz.getDeclaredField(name);
            }
        }
    }

    public static Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
        // Try to walk up the class hierarchy to find the field, also interfaces
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            try {
                return getDeclaredField(currentClass, name);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }

        throw new NoSuchFieldException();
    }

    static ClassLoader getClassLoader(Class<?> clazz) {
        if (clazz == null) return null;
        return clazz.getClassLoader();
    }
}

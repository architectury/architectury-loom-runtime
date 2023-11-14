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

package dev.architectury.reflectionredirector;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public class ReflectionRedirector {
    private static final Map<Integer, Map<String, UnaryOperator<AbstractInsnNode>>> REPLACERS = new HashMap<>();
    private static final String REFLECTION_RUNTIME = "dev/architectury/reflectionredirector/runtime/ArchitecturyReflectionRuntime";
    
    static {
        // Redirect Class.forName to ArchitecturyReflectionRuntime#forName
        replaceStaticInvoke("java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;",
                REFLECTION_RUNTIME, "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
        replaceStaticInvoke("java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;",
                REFLECTION_RUNTIME, "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;");
        // Redirect Class.getDeclaredMethod to ArchitecturyReflectionRuntime#getDeclaredMethod
        replaceVirtualInvokeAsStatic("java/lang/Class", "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
                REFLECTION_RUNTIME, "getDeclaredMethod", "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");
        // Redirect Class.getMethod to ArchitecturyReflectionRuntime#getMethod
        replaceVirtualInvokeAsStatic("java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
                REFLECTION_RUNTIME, "getMethod", "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");
        // Redirect Class.getDeclaredField to ArchitecturyReflectionRuntime#getDeclaredField
        replaceVirtualInvokeAsStatic("java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;",
                REFLECTION_RUNTIME, "getDeclaredField", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/reflect/Field;");
        // Redirect Class.getField to ArchitecturyReflectionRuntime#getField
        replaceVirtualInvokeAsStatic("java/lang/Class", "getField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;",
                REFLECTION_RUNTIME, "getField", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/reflect/Field;");
    }
    
    public static void redirect(ClassNode node) {
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insnNode : method.instructions) {
                Map<String, UnaryOperator<AbstractInsnNode>> operatorMap = REPLACERS.get(insnNode.getOpcode());
                
                if (operatorMap != null) {
                    UnaryOperator<AbstractInsnNode> operator = null;
                    
                    if (insnNode instanceof MethodInsnNode) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                        operator = operatorMap.get(methodInsnNode.owner + "." + methodInsnNode.name + methodInsnNode.desc);
                    }
                    
                    if (operator != null) {
                        AbstractInsnNode replacement = operator.apply(insnNode);
                        
                        if (replacement != null && replacement != insnNode) {
                            method.instructions.set(insnNode, replacement);
                        }
                    }
                }
            }
        }
    }
    
    private static <T extends AbstractInsnNode> void replace(int opcode, String key, UnaryOperator<T> replacer) {
        REPLACERS.computeIfAbsent(opcode, i -> new HashMap<>())
                .put(key, (UnaryOperator<AbstractInsnNode>) replacer);
    }
    
    private static void replaceStaticInvoke(String owner, String name, String descriptor, UnaryOperator<MethodInsnNode> replacer) {
        replace(Opcodes.INVOKESTATIC, owner + "." + name + descriptor, replacer);
    }
    
    private static void replaceStaticInvoke(String owner, String name, String descriptor, String replacerOwner, String replacerName, String replacerDescriptor) {
        replaceStaticInvoke(owner, name, descriptor, insn -> {
            insn.owner = replacerOwner;
            insn.name = replacerName;
            insn.desc = replacerDescriptor;
            return insn;
        });
    }
    
    private static void replaceVirtualInvokeAsStatic(String owner, String name, String descriptor, UnaryOperator<MethodInsnNode> replacer) {
        replace(Opcodes.INVOKEVIRTUAL, owner + "." + name + descriptor, replacer);
    }
    
    private static void replaceVirtualInvokeAsStatic(String owner, String name, String descriptor, String replacerOwner, String replacerName, String replacerDescriptor) {
        replaceVirtualInvokeAsStatic(owner, name, descriptor, insn -> {
            insn.setOpcode(Opcodes.INVOKESTATIC);
            insn.owner = replacerOwner;
            insn.name = replacerName;
            insn.desc = replacerDescriptor;
            return insn;
        });
    }
}

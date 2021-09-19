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

package dev.architectury.loom.forgeruntime.mixin;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;

import java.nio.file.Path;
import java.util.*;

public class ForgeLoomMixinRemapperInjectorService implements ITransformationService {
	@Override
	public String name() {
		return "ForgeLoomMixinRemapperInjector";
	}

	@Override
	public void initialize(IEnvironment environment) {
	}

	public void attach() {
		try {
			// Call via reflection so it doesn't crash if mixin doesn't exist at all
			Class.forName("dev.architectury.loom.forgeruntime.mixin.ForgeLoomMixinRemapperInjectorServiceImpl").getDeclaredMethod("attach")
					.invoke(null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	// ML 6
	public List<Map.Entry<String, Path>> runScan(IEnvironment environment) {
		attach();
		return new ArrayList<>();
	}

	// ML 9
	public List beginScanning(IEnvironment environment) {
		attach();
		return new ArrayList<>();
	}

	@Override
	public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
	}

	@Override
	public List<ITransformer> transformers() {
		return Collections.emptyList();
	}
}

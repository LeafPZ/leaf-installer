// spotless:off

/*
 * Copyright 2026 aoqia
 *
 * This file was originally released under LICENSE_UPSTREAM (see below for the original header) and has been relicensed
 * to GNU's AGPLv3 license, with additional terms in the LICENSE_EXCEPTION.md file.
 *
 * All modifications made to this file after the original version were created separate to the original copyright.
 * These modifications are provided under the AGPLv3 license (including exception terms). The portions of this file that
 * were not modified remain subject to the original copyright notice and license, and the original LICENSE is kept in
 * LICENSE_UPSTREAM located alongside LICENSE.
 *
 * The complete record of changes to this file is publicly available in the project's Git history, which anyone may view
 * at any time for any reason.
 *
 * Copyright (c) 2016-2025 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// spotless:on

package dev.aoqia.leaf.installer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipError;
import java.util.zip.ZipFile;

import com.dslplatform.json.CompiledJson;

import dev.aoqia.leaf.installer.util.Utils;

public final class LoaderVersion {
    public final String name;
    public final Path path;

    public LoaderVersion(String name) {
        this.name = name;
        this.path = null;
    }

    public LoaderVersion(Path path) throws IOException {
        try (ZipFile zf = new ZipFile(path.toFile())) {
            ZipEntry entry = zf.getEntry("leaf.mod.json");
            if (entry == null) {
                throw new FileNotFoundException("leaf.mod.json");
            }

            String modJsonContent;

            try (InputStream is = zf.getInputStream(entry)) {
                modJsonContent = Utils.readString(is);
            }

            this.name = Utils.deserializeJson(modJsonContent, ModJson.class).version();
        } catch (ZipError e) {
            throw new IOException(e);
        }

        this.path = path;
    }

    @CompiledJson
    private record ModJson(String version) {}
}

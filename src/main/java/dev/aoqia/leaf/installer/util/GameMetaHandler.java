/*
 * Copyright (c) 2016-2025 FabricMC, aoqia
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
package dev.aoqia.leaf.installer.util;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import dev.aoqia.leaf.installer.util.json.GameManifestVersion;
import dev.aoqia.leaf.installer.util.json.VersionTable;

public class GameMetaHandler extends MetaHandler<GameMetaHandler.Version> {
    private List<Version> versions;

    public GameMetaHandler(String metaPath) {
        super(metaPath);
    }

    @Override
    public void load() throws IOException {
        VersionTable versionTable = LeafService.queryMetaJson(getMetaPath(), VersionTable.class);
        this.versions = versionTable.versions().keySet().stream().map(Version::new).toList();
        complete(this.versions);
    }

    public Version getLatestVersion(boolean prerelease) {
        if (versions.isEmpty()) {
            throw new RuntimeException("no versions available at " + getMetaPath());
        }

        if (prerelease) {
            for (Version version : versions) {
                if (version.isPrerelease()) {
                    return version;
                }
            }
        }

        return versions.get(0);
    }

    public Version parseVersion(String value, boolean prerelease) {
        if (value == null || value.isEmpty() || value.equalsIgnoreCase("latest")) {
            return getLatestVersion(prerelease);
        } else {
            for (Version version : versions) {
                if (version.id.equals(value)) {
                    return version;
                }
            }

            return null;
        }
    }

    public List<Version> getVersions() {
        return Collections.unmodifiableList(versions);
    }

    public record Version(String id) {
        public Version(GameManifestVersion version) {
            this(version.id);
        }

        public boolean isPrerelease() {
            return id.contains("pre-release");
        }
    }
}

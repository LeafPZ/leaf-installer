/*
 * Copyright (C) 2026 aoqia
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see <https://www.gnu.org/licenses/>.
 *
 * This license and file are additionally subjected to the exceptions described in LICENSE_EXCEPTION.md.
 *
 * The exceptions permit independent software to use this project as a dependency under terms of its own choosing, but
 * does not permit the copying or incorporation of covered source code into another work outside the terms of the AGPL.
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

    public List<Version> getVersions() {
        return Collections.unmodifiableList(versions);
    }

    @Override
    public void load() throws IOException {
        VersionTable versionTable = LeafService.queryMetaJson(getMetaPath(), VersionTable.class);
        this.versions = versionTable.versions().keySet().stream().map(Version::new).toList();
        complete(this.versions);
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

    public record Version(String id) {
        public Version(GameManifestVersion version) {
            this(version.id);
        }

        public boolean isPrerelease() {
            return id.contains("pre-release");
        }
    }
}

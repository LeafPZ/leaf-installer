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

import java.util.Collections;
import java.util.List;

public abstract class LoaderMetaHandler extends MetaHandler<LoaderMetaHandler.Version> {
    private List<Version> versions;

    public LoaderMetaHandler(String metaPath) {
        super(metaPath);
    }

    public Version getLatestVersion() {
        if (versions.isEmpty()) {
            throw new RuntimeException("no versions available at " + getMetaPath());
        }

        return versions.get(0);
    }

    public Version parseVersion(String value, boolean snapshot) {
        if (value == null || value.isEmpty() || value.equalsIgnoreCase("latest")) {
            return getLatestVersion();
        } else {
            for (Version version : versions) {
                if (version.id().equals(value)) {
                    return version;
                }
            }

            return null;
        }
    }

    public List<Version> getVersions() {
        return Collections.unmodifiableList(versions);
    }

    public void setVersions(List<Version> versions) {
        this.versions = versions;
    }

    public record Version(String id) {}
}

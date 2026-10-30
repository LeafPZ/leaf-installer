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

public class Reference {
    public static final String DEFAULT_MAVEN_SERVER = "https://maven.aoqia.dev/releases/";
    public static final String DEFAULT_API_SERVER = "https://maven.aoqia.dev/api/";
    static final String MANIFEST_REPOSITORY = "https://raw.githubusercontent.com/LeafPZ/leaf/refs/heads/main/";
    public static final String ZOMBOID_VERSION_MANIFEST = "dist/manifests/index.json";
    static final String DEFAULT_META_SERVER = MANIFEST_REPOSITORY;
    static final LeafService[] LEAF_SERVICES = {
        new LeafService(DEFAULT_META_SERVER, DEFAULT_MAVEN_SERVER, DEFAULT_API_SERVER),
    };
}

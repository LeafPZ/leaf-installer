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

package dev.aoqia.leaf.installer.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public final class LeafService {
    private static int activeIndex = 0; // index into INSTANCES or -1 if set to a fixed service
    private static LeafService fixedService;

    private final String meta;
    private final String maven;
    private final String api;

    LeafService(String meta, String maven, String api) {
        this.meta = meta;
        this.maven = maven;
        this.api = api;
    }

    /**
     * Query Leaf Meta path and decode as JSON.
     */
    public static <T> T queryMetaJson(String path, Class<T> clazz) throws IOException {
        return invokeWithFallbacks(
            (service, arg) -> Utils.deserializeJson(HttpClient.readString(URI.create(service.meta + arg).toURL()),
                clazz), path);
    }

    /**
     * Query API path and decode as JSON.
     */
    public static <T> T queryApiJson(String path, Class<T> clazz) throws IOException {
        return invokeWithFallbacks(
            (service, arg) -> Utils.deserializeJson(HttpClient.readString(URI.create(service.api + arg).toURL()),
                clazz), path);
    }

    /**
     * Query and decode JSON from url, substituting aoqia maven with fallbacks or overrides.
     */
    public static <T> T queryJsonSubstitutedMaven(String url, Class<T> clazz) throws IOException {
        if (!url.startsWith(Reference.DEFAULT_MAVEN_SERVER)) {
            return Utils.deserializeJson(HttpClient.readString(URI.create(url).toURL()), clazz);
        }

        String path = url.substring(Reference.DEFAULT_MAVEN_SERVER.length());

        return invokeWithFallbacks(
            (service, arg) -> Utils.deserializeJson(HttpClient.readString(URI.create(service.meta + arg).toURL()),
                clazz), path);
    }

    public static void downloadLatestGitHubRelease(String repoOwner, String repoName, Path out) throws
        IOException, InterruptedException
    {
        String tag = getLatestGitHubRelease(repoOwner, repoName);
        String version = tag.startsWith("v") ? tag.substring(1) : tag;

        String filename = String.format("native-%s.dll", version);

        downloadSubstitutedMaven(
            String.format("https://github.com/%s/%s/releases/download/%s/%s", repoOwner, repoName, tag, filename),
            out.resolve(filename));
    }

    public static String getLatestGitHubRelease(String repoOwner, String repoName) throws IOException,
        InterruptedException
    {
        return Utils.getGitHubReleaseTagForRepo(
            URI.create(String.format("https://github.com/%s/%s/releases/latest", repoOwner, repoName)));
    }

    /**
     * Download url to file, substituting aoqia maven with fallbacks or overrides.
     */
    public static void downloadSubstitutedMaven(String url, Path out) throws IOException {
        if (!url.startsWith(Reference.DEFAULT_MAVEN_SERVER)) {
            HttpClient.downloadFile(URI.create(url).toURL(), out);
            return;
        }

        String path = url.substring(Reference.DEFAULT_MAVEN_SERVER.length());

        invokeWithFallbacks((service, arg) -> {
            HttpClient.downloadFile(URI.create(service.maven + arg).toURL(), out);
            return null;
        }, path);
    }

    private static <A, R> R invokeWithFallbacks(Handler<A, R> handler, A arg) throws IOException {
        if (fixedService != null) {
            return handler.apply(fixedService, arg);
        }

        int index = activeIndex;
        IOException exc = null;

        do {
            LeafService service = Reference.LEAF_SERVICES[index];

            try {
                R ret = handler.apply(service, arg);
                activeIndex = index;

                return ret;
            } catch (IOException e) {
                System.out.println("service " + service + " failed: " + e);

                if (exc == null) {
                    exc = e;
                } else {
                    exc.addSuppressed(e);
                }
            }

            index = (index + 1) % Reference.LEAF_SERVICES.length;
        } while (index != activeIndex);

        throw exc;
    }

    /**
     * Configure fixed service urls, disabling fallbacks or the defaults.
     */
    public static void setFixed(String metaUrl, String mavenUrl, String apiUrl) {
        if (metaUrl == null && mavenUrl == null && apiUrl == null) {
            throw new NullPointerException("all of meta/maven/api are null");
        }

        if (metaUrl == null) {
            metaUrl = Reference.DEFAULT_META_SERVER;
        }
        if (mavenUrl == null) {
            mavenUrl = Reference.DEFAULT_MAVEN_SERVER;
        }
        if (apiUrl == null) {
            apiUrl = Reference.DEFAULT_API_SERVER;
        }

        activeIndex = -1;
        fixedService = new LeafService(metaUrl, mavenUrl, apiUrl);
    }

    public String getMetaUrl() {
        return meta;
    }

    public String getMavenUrl() {
        return maven;
    }

    @Override
    public String toString() {
        return "LeafService{" + "meta='" + meta + '\'' + ", maven='" + maven + "'}";
    }

    private interface Handler<A, R> {
        R apply(LeafService service, A arg) throws IOException;
    }
}

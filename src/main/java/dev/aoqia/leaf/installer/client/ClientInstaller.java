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
package dev.aoqia.leaf.installer.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

import org.apache.commons.collections4.iterators.IteratorChain;

import dev.aoqia.leaf.installer.LoaderVersion;
import dev.aoqia.leaf.installer.util.InstallerProgress;
import dev.aoqia.leaf.installer.util.LaunchConfigUtil;
import dev.aoqia.leaf.installer.util.LeafService;
import dev.aoqia.leaf.installer.util.Library;
import dev.aoqia.leaf.installer.util.OperatingSystem;
import dev.aoqia.leaf.installer.util.Reference;
import dev.aoqia.leaf.installer.util.Utils;
import dev.aoqia.leaf.installer.util.json.LoaderJson;

public class ClientInstaller {
    private final Path gameDir;
    private final String gameVersion;
    private final Path leafLibDir;
    private final LoaderVersion loaderVersion;
    private final InstallerProgress progress;

    public ClientInstaller(Path gameDir, String gameVersion, LoaderVersion loaderVersion, InstallerProgress progress) {
        this.gameDir = gameDir;
        this.gameVersion = gameVersion;
        this.loaderVersion = loaderVersion;
        this.progress = progress;

        this.leafLibDir = gameDir.resolve(".leaf/lib");
    }

    public void install(boolean proxy, boolean createConfig) throws IOException {
        if (proxy) {
            System.out.printf("Installing leaf-loader-proxy for %s%n", gameVersion);
        } else {
            System.out.printf("Installing leaf-loader %s for %s%n", loaderVersion.name, gameVersion);
        }

        progress.updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.installing"))
            .format(new Object[] { (proxy ? "Proxy " : "") + loaderVersion.name }));

        var proxyLib = Utils.getLatestLoaderProxy();

        String configName = String.format("leaf-%s-%s", proxy ? proxyLib.version : loaderVersion.name, gameVersion);

        if (Files.exists(this.leafLibDir)) {
            Utils.deleteDirectory(this.leafLibDir);
        }
        Files.createDirectories(this.leafLibDir);

        if (proxy) {
            LeafService.downloadSubstitutedMaven(proxyLib.getURL(),
                leafLibDir.resolve("%s-%s.jar".formatted(proxyLib.artifactId, proxyLib.version)));

            // Also download the proxy native on Windows which fixes a DLL load order issue
            if (OperatingSystem.CURRENT == OperatingSystem.WINDOWS) {
                try {
                    LeafService.downloadLatestGitHubRelease("LeafPZ", "leaf-loader-proxy-native", leafLibDir);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Failed to download proxy native from GitHub", e);
                }
            }
        } else {
            LoaderJson loaderVersionJson = LeafService.queryMetaJson(
                "dist/loader/%s.json".formatted(loaderVersion.name), LoaderJson.class);
            LoaderJson.Libraries libsJson = loaderVersionJson.libraries();
            String mainClass = loaderVersionJson.mainClass().client();
            String mainClassInternal = mainClass.replace(".", "/");

            // Putting loader dependency into the libs list for later download.
            libsJson
                .common()
                .add(new LoaderJson.Library("dev.aoqia.leaf:loader:" + loaderVersion.name,
                    Reference.DEFAULT_MAVEN_SERVER,
                    null, null, null, null, null));

            final var libs = new IteratorChain<>(libsJson.common().iterator(), libsJson.client().iterator());
            libs.forEachRemaining(libJson -> {
                Library library = new Library(libJson);
                Path libraryFile = leafLibDir.resolve("%s-%s.jar".formatted(library.artifactId, library.version));

                progress.updateProgress(
                    new MessageFormat(Utils.BUNDLE.getString("progress.download.library.entry")).format(
                        new Object[] { library.dependency }));

                try {
                    LeafService.downloadSubstitutedMaven(library.getURL(), libraryFile);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to download library " + library.artifactId, e);
                }
            });

            if (createConfig) {
                var launchConfigUtil = new LaunchConfigUtil(gameDir);
                launchConfigUtil.createConfig(configName, mainClassInternal);
            }
        }

        progress.updateProgress(Utils.BUNDLE.getString("progress.done"));
    }
}

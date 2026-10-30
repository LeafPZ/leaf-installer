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

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.runtime.Settings;

import dev.aoqia.leaf.installer.client.ClientHandler;
import dev.aoqia.leaf.installer.server.ServerHandler;
import dev.aoqia.leaf.installer.util.ArgumentParser;
import dev.aoqia.leaf.installer.util.CrashDialog;
import dev.aoqia.leaf.installer.util.GameMetaHandler;
import dev.aoqia.leaf.installer.util.GithubMetaHandler;
import dev.aoqia.leaf.installer.util.LeafService;
import dev.aoqia.leaf.installer.util.OperatingSystem;
import dev.aoqia.leaf.installer.util.Reference;

public class Main {
    public static final List<Handler> HANDLERS = new ArrayList<>();
    public static final DslJson<Object> JSON = new DslJson<>(Settings.withRuntime().includeServiceLoader());

    public static GameMetaHandler GAME_VERSION_META;
    public static GithubMetaHandler LOADER_META;

    public static String LATEST_PROXY_NATIVE_TAG = null;

    public static void main(String[] args) {
        if (OperatingSystem.CURRENT == OperatingSystem.WINDOWS) {
            // Use the operating system cert store
            System.setProperty("javax.net.ssl.trustStoreType", "WINDOWS-ROOT");
        }

        System.setProperty("java.net.useSystemProxies", "true");

        System.out.println("Loading Leaf Installer: " + Main.class.getPackage().getImplementationVersion());

        HANDLERS.add(new ClientHandler());
        HANDLERS.add(new ServerHandler());

        ArgumentParser argumentParser = ArgumentParser.create(args);
        String command = argumentParser.getCommand().orElse(null);

        // Can be used if you wish to re-host or provide custom versions.
        // Ensure you include the trailing /
        String metaUrl = argumentParser.get("metaurl");
        String mavenUrl = argumentParser.get("mavenurl");
        String apiUrl = argumentParser.get("apiurl");
        if (metaUrl != null || mavenUrl != null) {
            LeafService.setFixed(metaUrl, mavenUrl, apiUrl);
        }

        GAME_VERSION_META = new GameMetaHandler(Reference.ZOMBOID_VERSION_MANIFEST);
        LOADER_META = new GithubMetaHandler("LeafPZ", "leaf", "main", "dist/loader");

        if (OperatingSystem.CURRENT == OperatingSystem.WINDOWS) {
            try {
                LATEST_PROXY_NATIVE_TAG = LeafService.getLatestGitHubRelease("LeafPZ", "leaf-loader-proxy-native");
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Failed to get latest proxy native tag", e);
            }
        }

        // Default to the help command in a headless environment
        if (GraphicsEnvironment.isHeadless() && command == null) {
            command = "help";
        }

        if (command == null) {
            try {
                InstallerGui.start();
            } catch (Exception e) {
                e.printStackTrace();
                new CrashDialog(e);
            }
        } else if (command.equals("help")) {
            System.out.println("help - Opens this menu");
            HANDLERS.forEach(handler -> System.out.printf("%s %s\n", handler.name().toLowerCase(), handler.cliHelp()));
            loadMetadata();

            System.out.printf("\nLatest Version: %s\nLatest Loader: %s\n",
                GAME_VERSION_META.getLatestVersion(argumentParser.has("prerelease")).id(),
                LOADER_META.getLatestVersion().id());
        } else {
            loadMetadata();

            for (Handler handler : HANDLERS) {
                if (command.equalsIgnoreCase(handler.name())) {
                    try {
                        handler.installCli(argumentParser);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to install " + handler.name(), e);
                    }

                    return;
                }
            }

            System.out.println("No handler found for " + args[0] + " see help");
        }
    }

    public static void loadMetadata() {
        try {
            LOADER_META.load();
            GAME_VERSION_META.load();
        } catch (Throwable t) {
            throw new RuntimeException("Unable to load metadata", t);
        }
    }
}

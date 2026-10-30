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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import dev.aoqia.leaf.installer.util.json.LauncherConfig;

public class LaunchConfigUtil {
    private static final String ORIGINAL_CONFIG_NAME = "ProjectZomboid64.json";

    private final Path gameDir;
    private final Path leafLibDir;

    public LaunchConfigUtil(Path gameDir) {
        this.gameDir = gameDir;
        this.leafLibDir = gameDir.resolve(Utils.LEAF_FOLDER).resolve("lib");
    }

    private static Path getLaunchScriptPath(Path gameDir) {
        return switch (OperatingSystem.CURRENT) {
            case WINDOWS -> gameDir.resolve("StartServer64.bat");
            case LINUX -> gameDir.resolve("start-server.sh");
            case MACOS -> throw new RuntimeException("MacOS launch scripts not implemented.");
        };
    }

    public void createConfig(String name, String mainClass) throws IOException {
        final var originalConfig = this.gameDir.resolve(ORIGINAL_CONFIG_NAME);
        final var newConfig = this.gameDir.resolve(name + ".json");

        if (Files.notExists(originalConfig)) {
            throw new RuntimeException(Utils.BUNDLE.getString("progress.exception.no.launcher.config"));
        }

        try {
            Files.copy(originalConfig, newConfig, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy bootstrapper config: ", e);
        }

        LauncherConfig newConfigJson;
        try {
            newConfigJson = Utils.deserializeJson(Files.readAllBytes(newConfig), LauncherConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read bootstrapper config: ", e);
        }

        newConfigJson.mainClass = mainClass;

        // Add our loader's libraries to the classpath.
        // Java 6+ supports cp wildcards but the bootstrapper hard crashes with them.
        try (Stream<Path> stream = Files.walk(leafLibDir).filter(Files::isRegularFile)) {
            stream.forEach(path -> newConfigJson.classpath.add(this.gameDir.relativize(path).toString()));
        }

        try {
            Utils.serializeJson(newConfigJson, Files.newOutputStream(newConfig));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write bootstrapper config: ", e);
        }
    }

    public void createScript(String name, String mainClass) throws IOException {
        final var template = getLaunchScriptPath(this.gameDir);
        if (Files.notExists(template)) {
            throw new RuntimeException("Server launch script doesn't exist.");
        }

        final var templateData = Files.readString(template);
        if (OperatingSystem.CURRENT == OperatingSystem.WINDOWS) {
            createScriptWindows(name, mainClass, templateData);
        } else if (OperatingSystem.CURRENT == OperatingSystem.LINUX) {
            createScriptLinux(name, mainClass, templateData);
        } else {
            throw new RuntimeException(
                "Server launch script support not implemented for this OS. Please raise an issue "
                    + "on leaf-installer's GitHub repository!");
        }
    }

    private void createScriptWindows(String name, String mainClass, String templateData) throws IOException {
        final var pattern = Pattern.compile("^SET\\s*PZ_CLASSPATH=(.+)[\\r\\n]+.*-cp.*(zombie(?:\\.\\w+)+)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

        final var matcher = pattern.matcher(templateData);
        if (!matcher.find()) {
            throw new RuntimeException("Failed to find match regex in launch script");
        }

        var libs = new StringBuilder(matcher.group(1));
        try (final var stream = Files.walk(this.leafLibDir).filter(Files::isRegularFile)) {
            stream.forEach(lib -> libs.append(this.gameDir.relativize(lib)).append(";"));
        }

        final var newScript = templateData.replace(matcher.group(1), libs).replace(matcher.group(2), mainClass);
        final var out = gameDir.resolve(name + ".bat");
        Files.writeString(out, newScript);

        out.toFile().setExecutable(true, false);
    }

    /*
     * Don't use this function by any means currently.
     * Linux servers don't need to edit any scripts.
     * Instead, only configs should be edited because start-server.sh acts as the bootstrapper.
     */
    private void createScriptLinux(String name, String mainClass, String templateData) throws IOException {
        var newScript = templateData;

        // Edit classpath
        {
            final var pattern = Pattern.compile("^(\\s*export\\s+LD_LIBRARY_PATH=\").+\"$",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

            final var matcher = pattern.matcher(templateData);
            if (!matcher.find()) {
                throw new RuntimeException("Failed to find match regex in launch script");
            }

            newScript = newScript.replace(matcher.group(1),
                matcher.group(1) + "${INSTDIR}/" + Utils.LEAF_FOLDER + "/libraries:");
        }

        final var out = gameDir.resolve(name + ".sh");
        Files.writeString(out, newScript);

        out.toFile().setExecutable(true, false);
    }
}

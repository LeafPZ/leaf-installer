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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.aoqia.leaf.installer.util.json.LauncherConfig;

public final class ClientLaunchOptions {
    private static final Pattern LEAF_ARGUMENTS = Pattern.compile(
        "(?i)(?<!\\S)(?:-agentpath:(?:\"[^\"]*\\.leaf[/\\\\]lib[/\\\\]native-[^\"]*\"|\\.leaf[/\\\\]lib[/\\\\]native-\\S+)"
            + "|-javaagent:(?:\"[^\"]*\\.leaf[/\\\\]lib[/\\\\]loader(?:-proxy)?-[^\"]*\"|\\.leaf[/\\\\]lib[/\\\\]loader(?:-proxy)?-\\S+)"
            + "|-pzexeconfig\\s+(?:\"leaf-[^\"]+\\.json\"|leaf-\\S+\\.json))(?=\\s|$)");

    private ClientLaunchOptions() {}

    public static String mergeArguments(String current, String arguments) {
        String remaining = LEAF_ARGUMENTS.matcher(current).replaceAll("").trim();
        if (arguments.endsWith(" --")) {
            String agents = arguments.substring(0, arguments.length() - 3);
            return (agents + (Pattern.compile("(?<!\\S)--(?!\\S)").matcher(remaining).find() ? " " : " -- ")
                + remaining).trim();
        }
        return arguments + (remaining.isEmpty() ? "" : " " + remaining);
    }

    public static void patchAlternateLauncher(Path gameDir, String agents, String mainClass) throws IOException {
        Path script = gameDir.resolve("ProjectZomboid64.bat");
        String content = Files.readString(script);
        if (mainClass == null) {
            LauncherConfig config = Utils.deserializeJson(Files.readAllBytes(gameDir.resolve("ProjectZomboid64.json")),
                LauncherConfig.class);
            mainClass = config.mainClass;
        }
        writeWithBackup(script, patchBatch(content, agents, mainClass));
    }

    public static String patchBatch(String content, String agents, String mainClass) throws IOException {
        String newline = content.contains("\r\n") ? "\r\n" : "\n";
        Pattern optionsPattern = assignmentPattern("_JAVA_OPTIONS");
        Matcher options = optionsPattern.matcher(content);
        if (options.find()) {
            String current = assignmentValue(options);
            String updated = LEAF_ARGUMENTS.matcher(current).replaceAll("").trim();
            updated = agents + (agents.isEmpty() || updated.isEmpty() ? "" : " ") + updated;
            content = options.replaceFirst(Matcher.quoteReplacement(assignment(options, "_JAVA_OPTIONS", updated)));
        } else if (!agents.isEmpty()) {
            Matcher local = Pattern.compile("(?im)^[^\\r\\n]*\\bsetlocal\\b[^\\r\\n]*(?:\\r?\\n|$)").matcher(content);
            int position = local.find() ? local.end() : 0;
            content = content.substring(0, position) + "SET _JAVA_OPTIONS=" + agents + newline + content.substring(position);
        }

        Matcher classpath = assignmentPattern("PZ_CLASSPATH").matcher(content);
        if (!classpath.find()) {
            throw new IOException("Could not find PZ_CLASSPATH in the alternative launcher.");
        }
        List<String> entries = new ArrayList<>(Arrays.asList(assignmentValue(classpath).split(";")));
        entries.removeIf(entry -> entry.trim().replace('\\', '/').matches("(?i)(?:\\./)?\\.leaf/lib(?:/.*)?"));
        if (agents.isEmpty()) {
            entries.add(".leaf/lib/*");
        }
        content = classpath.replaceFirst(Matcher.quoteReplacement(assignment(classpath, "PZ_CLASSPATH",
            String.join(";", entries))));
        Matcher launch = Pattern.compile("(?i)(-cp\\s+(?:\"[^\"]+\"|\\S+)\\s+)\\S+").matcher(content);
        if (!launch.find()) {
            throw new IOException("Could not find the Java command in the alternative launcher.");
        }
        return launch.replaceAll("$1" + Matcher.quoteReplacement(mainClass.replace('/', '.')));
    }

    private static Pattern assignmentPattern(String name) {
        return Pattern.compile("(?im)^([ \\t]*@?set[ \\t]+)(\"?)" + name + "=([^\\r\\n]*)$");
    }

    private static String assignmentValue(Matcher matcher) throws IOException {
        String value = matcher.group(3).trim();
        if (!matcher.group(2).isEmpty()) {
            if (!value.endsWith("\"")) {
                throw new IOException("Unclosed quoted batch assignment.");
            }
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String assignment(Matcher matcher, String name, String value) {
        return matcher.group(1) + matcher.group(2) + name + "=" + value + matcher.group(2);
    }

    static void writeWithBackup(Path path, String content) throws IOException {
        if (Files.readString(path).equals(content)) {
            return;
        }
        Path backup = path.resolveSibling(path.getFileName() + ".leaf-backup");
        if (Files.notExists(backup)) {
            Files.copy(path, backup);
        }
        Files.writeString(path, content);
    }
}

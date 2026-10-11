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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SteamLaunchOptions {
    private SteamLaunchOptions() {}

    public static void checkSteamClosed() throws IOException {
        try (var processes = ProcessHandle.allProcesses()) {
            if (processes.anyMatch(process -> process.info().command().map(command -> {
                String name = Path.of(command).getFileName().toString();
                return name.equalsIgnoreCase("steam.exe") || name.equalsIgnoreCase("steam");
            }).orElse(false))) {
                throw new IOException(Utils.BUNDLE.getString("progress.exception.steam.running"));
            }
        }
    }

    public static void update(Path gameDir, String arguments) throws IOException {
        checkSteamClosed();
        Path steamDir = findSteamDirectory(gameDir);
        List<Path> configs;
        try (var users = Files.list(steamDir.resolve("userdata"))) {
            configs = users.filter(Files::isDirectory).map(user -> user.resolve("config/localconfig.vdf"))
                .filter(Files::isRegularFile).toList();
        }
        if (configs.isEmpty()) {
            throw new IOException("No Steam user configurations found in " + steamDir);
        }
        List<String> updates = new ArrayList<>();
        for (Path config : configs) {
            updates.add(patchConfig(Files.readString(config), arguments));
        }
        checkSteamClosed();
        for (int i = 0; i < configs.size(); i++) {
            ClientLaunchOptions.writeWithBackup(configs.get(i), updates.get(i));
        }
    }

    private static Path findSteamDirectory(Path gameDir) throws IOException {
        List<Path> candidates = new ArrayList<>();
        if (OperatingSystem.CURRENT == OperatingSystem.WINDOWS) {
            for (String key : List.of("HKCU\\Software\\Valve\\Steam", "HKLM\\Software\\Valve\\Steam",
                "HKLM\\Software\\WOW6432Node\\Valve\\Steam")) {
                Process query = new ProcessBuilder("reg", "query", key, "/v", "SteamPath").redirectErrorStream(true).start();
                String output = new String(query.getInputStream().readAllBytes());
                Matcher path = Pattern.compile("SteamPath\\s+REG_SZ\\s+([^\\r\\n]+)", Pattern.CASE_INSENSITIVE)
                    .matcher(output);
                if (path.find()) {
                    candidates.add(Path.of(path.group(1).trim()));
                    break;
                }
            }
        }
        for (Path parent = gameDir.toAbsolutePath(); parent != null; parent = parent.getParent()) {
            candidates.add(parent);
        }
        candidates.add(Utils.getDefaultSteamLibraryPath());
        candidates.add(Path.of(System.getProperty("user.home"), ".local/share/Steam"));
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate.resolve("userdata"))) {
                return candidate;
            }
        }
        throw new IOException("Could not locate Steam's userdata directory.");
    }

    public static String patchConfig(String content, String arguments) throws IOException {
        Node apps = new Parser(content).parse();
        for (String key : List.of("UserLocalConfigStore", "Software", "Valve", "Steam", "Apps")) {
            apps = apps.child(key);
            if (apps == null || apps.children == null) {
                throw new IOException("Missing Steam configuration section: " + key);
            }
        }
        String newline = content.contains("\r\n") ? "\r\n" : "\n";
        Node app = apps.child("108600");
        if (app == null) {
            String entry = "\"108600\"" + newline + "\t\t\t\t\t{" + newline
                + "\t\t\t\t\t\t\"LaunchOptions\"\t\t" + quote(arguments) + newline + "\t\t\t\t\t}";
            return insert(content, apps.end, entry, newline);
        }
        if (app.children == null) {
            throw new IOException("Invalid Project Zomboid Steam configuration.");
        }
        Node options = app.child("LaunchOptions");
        if (options == null) {
            return insert(content, app.end, "\"LaunchOptions\"\t\t" + quote(arguments), newline);
        }
        if (options.children != null) {
            throw new IOException("Invalid Steam LaunchOptions value.");
        }
        String updated = ClientLaunchOptions.mergeArguments(options.value, arguments);
        if (updated.equals(options.value)) {
            return content;
        }
        return content.substring(0, options.start) + quote(updated) + content.substring(options.end);
    }

    private static String insert(String content, int position, String entry, String newline) {
        int lineStart = content.lastIndexOf('\n', position - 1) + 1;
        String indent = content.substring(lineStart, position);
        if (!indent.isBlank()) {
            return content.substring(0, position) + newline + entry + newline + content.substring(position);
        }
        return content.substring(0, lineStart) + indent + "\t" + entry + newline + content.substring(lineStart);
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private record Node(String key, String value, List<Node> children, int start, int end) {
        Node child(String name) {
            return children.stream().filter(node -> node.key.equalsIgnoreCase(name)).findFirst().orElse(null);
        }
    }

    private static final class Parser {
        private final String content;
        private final Matcher tokens;
        private String token;
        private int start;
        private int end;

        Parser(String content) {
            this.content = content;
            tokens = Pattern.compile("\\G(?:[\\s\\uFEFF]++|//[^\\r\\n]*+)*+(\"(?:\\\\.|[^\"\\\\])*+\"|[{}]|[^\\s{}\"]++)")
                .matcher(content);
        }

        Node parse() throws IOException {
            advance();
            Node root = new Node("", "", entries(false), 0, content.length());
            if (token != null) {
                throw new IOException("Unexpected closing brace in Steam configuration.");
            }
            return root;
        }

        private List<Node> entries(boolean nested) throws IOException {
            List<Node> result = new ArrayList<>();
            while (token != null && !token.equals("}")) {
                if (token.equals("{")) {
                    throw new IOException("Missing key in Steam configuration.");
                }
                String key = decode(token);
                advance();
                if (token == null || token.equals("}")) {
                    throw new IOException("Missing value for Steam configuration key " + key);
                }
                int valueStart = start;
                if (token.equals("{")) {
                    advance();
                    List<Node> children = entries(true);
                    result.add(new Node(key, null, children, valueStart, start));
                } else {
                    result.add(new Node(key, decode(token), null, valueStart, end));
                }
                advance();
            }
            if (nested && token == null) {
                throw new IOException("Unclosed Steam configuration section.");
            }
            return result;
        }

        private void advance() throws IOException {
            if (tokens.find()) {
                token = tokens.group(1);
                start = tokens.start(1);
                end = tokens.end(1);
            } else {
                token = null;
                if (!content.substring(end).replaceAll("//[^\\r\\n]*", "").isBlank()) {
                    throw new IOException("Invalid Steam configuration syntax.");
                }
            }
        }

        private static String decode(String token) {
            if (!token.startsWith("\"")) {
                return token;
            }
            return token.substring(1, token.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
    }
}

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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class ArgumentParser {
    private final String[] args;
    private Map<String, String> argMap;
    // The command will be the first argument passed, and if it doesnt start with -
    private String command = null;

    private ArgumentParser(String[] args) {
        this.args = args;
        parse();
    }

    public static ArgumentParser create(String[] args) {
        return new ArgumentParser(args);
    }

    public String get(String argument) {
        return argMap.get(argument);
    }

    public Optional<String> getCommand() {
        return command == null ? Optional.empty() : Optional.of(command);
    }

    public String getOrDefault(String argument, Supplier<String> stringSuppler) {
        String ret = argMap.get(argument);

        if (ret == null) {
            ret = stringSuppler.get();
        }

        return ret;
    }

    public boolean has(String argument) {
        return argMap.containsKey(argument);
    }

    private void parse() {
        argMap = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                String key = args[i].substring(1);
                String value = null;

                // If it's an arg like -test=123
                if (args[i].contains("=")) {
                    final String truekey = key.substring(0, key.indexOf('='));
                    value = key.substring(key.indexOf('=') + 1);
                    argMap.put(truekey, value);
                    continue;
                }

                if (argMap.containsKey(key)) {
                    throw new IllegalArgumentException(String.format("Argument %s already passed", key));
                }

                argMap.put(key, value);
            } else if (i == 0) {
                command = args[i];
            }
        }
    }
}

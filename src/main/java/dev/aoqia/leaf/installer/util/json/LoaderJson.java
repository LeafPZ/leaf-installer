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

package dev.aoqia.leaf.installer.util.json;

import java.util.List;

import com.dslplatform.json.CompiledJson;

@CompiledJson
public record LoaderJson(Libraries libraries, MainClass mainClass) {
    @CompiledJson
    public record Libraries(
        List<Library> client,
        List<Library> common,
        List<Library> server,
        List<Library> development
    )
    {}

    @CompiledJson
    public record Library(String name, String url, String md5, String sha1, String sha256, String sha512, Long size) {}

    @CompiledJson
    public record MainClass(String client, String server) {}
}

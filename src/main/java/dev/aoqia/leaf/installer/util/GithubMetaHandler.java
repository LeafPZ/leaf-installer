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
import java.util.stream.Collectors;

import dev.aoqia.leaf.installer.util.json.github.TreeCommitInfo;

/**
 * The primary use of this class is to parse git trees of a repository.
 * For example, this is used to get all the files in a specific folder under a GitHub repository.
 */
public class GithubMetaHandler extends LoaderMetaHandler {
    public GithubMetaHandler(String repoOwner, String repoName, String branch) {
        super(String.format("https://github.com/%s/%s/tree-commit-info/%s", repoOwner, repoName, branch));
    }

    public GithubMetaHandler(String repoOwner, String repoName, String branch, String subpath) {
        super(String.format("https://github.com/%s/%s/tree-commit-info/%s/%s", repoOwner, repoName, branch, subpath));
    }

    @Override
    public void load() throws IOException {
        TreeCommitInfo info = LeafService.queryJsonSubstitutedMaven(getMetaPath(), TreeCommitInfo.class);

        setVersions(info.entries()
            .keySet()
            .stream()
            .map(s -> s.replace(".json", ""))
            .map(Version::new)
            .sorted((v1, v2) -> v2.id().compareToIgnoreCase(v1.id()))
            .collect(Collectors.toList()));

        complete(getVersions());
    }
}

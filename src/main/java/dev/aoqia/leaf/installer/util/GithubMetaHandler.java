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

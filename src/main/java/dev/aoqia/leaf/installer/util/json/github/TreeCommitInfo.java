package dev.aoqia.leaf.installer.util.json.github;

import java.util.Map;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;

@CompiledJson
public record TreeCommitInfo(Map<String, Entry> entries) {
    /**
     * @param oid the full commit hash
     * @param url the sub-url related to the commit
     * @param date the date related to the commit
     */
    @CompiledJson
    public record Entry(
        @JsonAttribute(ignore = true) String oid,
        String url,
        String date,
        @JsonAttribute(ignore = true) Object shortMessageHtmlLink) {}
}

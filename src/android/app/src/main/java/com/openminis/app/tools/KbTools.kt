package com.openminis.app.tools

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * kb_search / kb_read / kb_list — native Kotlin port of the JS plugin
 * assets/builtin_plugins/com.xctzai.pojia/main.js. Same scoring (6×title +
 * 4×cve + body) so results match the JS version byte-for-byte, but no JS
 * engine dependency and no 350KB eval on first call.
 *
 * Data is read from the pojia-kb skill's kb.json (installed by
 * SkillRepository.installAssetSkills from assets/bundled_skills/pojia-kb).
 * Falls back to reading the asset directly if the skill hasn't been
 * installed yet.
 */
object KbTools {
    private const val TAG = "KbTools"
    private const val SKILL_DIR = "/var/minis/skills/pojia-kb"
    private const val ASSET_PATH = "bundled_skills/pojia-kb/kb.json"

    private data class Entry(val cat: String, val id: String, val title: String,
                             val severity: String, val cve: String, val body: String)

    @Volatile private var cached: Pair<List<Entry>, Map<String, String>>? = null

    @Synchronized
    private fun load(context: Context): Pair<List<Entry>, Map<String, String>> {
        cached?.let { return it }
        val json = runCatching {
            java.io.File("$SKILL_DIR/kb.json").takeIf { it.isFile }?.readText()
        }.getOrNull() ?: runCatching {
            context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        }.getOrElse {
            Log.e(TAG, "kb.json not found in skill dir or assets", it)
            return Pair(emptyList(), emptyMap())
        }
        val root = JSONObject(json)
        val labelsJson = root.getJSONObject("labels")
        val labels = mutableMapOf<String, String>()
        labelsJson.keys().forEach { k -> labels[k] = labelsJson.getString(k) }
        val kb = root.getJSONObject("kb")
        val entries = mutableListOf<Entry>()
        kb.keys().forEach { cat ->
            val items = kb.getJSONObject(cat)
            items.keys().forEach { id ->
                val e = items.getJSONObject(id)
                entries.add(Entry(cat, id,
                    e.optString("t"), e.optString("s"), e.optString("c"), e.optString("b")))
            }
        }
        val result = Pair(entries, labels)
        cached = result
        Log.i(TAG, "kb loaded: ${entries.size} entries in ${labels.size} categories")
        return result
    }

    private fun tokenize(q: String): List<String> {
        val out = mutableListOf<String>()
        val lower = q.lowercase()
        Regex("[a-z0-9][a-z0-9._-]*").findAll(lower).forEach { out.add(it.value) }
        Regex("[\\u4e00-\\u9fff]+").findAll(lower).forEach { m ->
            val run = m.value
            if (run.length == 1) out.add(run)
            else for (i in 0 until run.length - 1) out.add(run.substring(i, i + 2))
        }
        return out
    }

    private fun countOccur(hay: String, needle: String): Int {
        if (needle.isEmpty()) return 0
        var c = 0; var i = 0
        while (true) {
            val j = hay.indexOf(needle, i)
            if (j < 0) break
            c++; i = j + needle.length
        }
        return c
    }

    private fun score(e: Entry, tokens: List<String>): Int {
        val title = e.title.lowercase()
        val body = e.body.lowercase()
        val cve = e.cve.lowercase()
        var s = 0
        for (w in tokens) {
            s += 6 * countOccur(title, w) + 4 * countOccur(cve, w) + minOf(countOccur(body, w), 10)
        }
        return s
    }

    private fun short(e: Entry) = "${e.cat}/${e.id}  [${e.severity}]  ${e.title}  (${e.cve})"

    private fun textResult(text: String, toolId: String): ToolExecutionResult =
        ToolExecutionResult(output = text, success = true, toolTitle = "PoJia KB")

    fun execute(toolName: String, argsJson: String, toolId: String, context: Context): ToolExecutionResult {
        val args = runCatching { JSONObject(argsJson) }.getOrElse { JSONObject() }
        val (entries, labels) = load(context)
        if (entries.isEmpty()) return textResult("kb.json unavailable — reinstall the pojia-kb skill", toolId)
        return when (toolName) {
            "kb_search" -> {
                val q = args.optString("query").ifBlank { args.optString("q") }
                if (q.isBlank()) return textResult("usage: kb_search {query: \"...\"}", toolId)
                val tokens = tokenize(q)
                val hits = entries.map { it to score(it, tokens) }.filter { it.second > 0 }
                    .sortedByDescending { it.second }.take(12)
                if (hits.isEmpty()) textResult("no hits for '$q'", toolId)
                else textResult(hits.joinToString("\n") { "%4d  %s".format(it.second, short(it.first)) }, toolId)
            }
            "kb_read" -> {
                val q = args.optString("id").ifBlank { args.optString("query") }
                if (q.isBlank()) return textResult("usage: kb_read {id: \"java-web/cve-2021-29441-nacos\"}", toolId)
                val exact = entries.firstOrNull { it.id == q || "${it.cat}/${it.id}" == q }
                if (exact != null) return textResult(
                    "# ${exact.title}\n\n- id: ${exact.cat}/${exact.id}\n- severity: ${exact.severity}\n- cve: ${exact.cve}\n\n${exact.body}", toolId)
                val tokens = tokenize(q)
                val best = entries.maxByOrNull { score(it, tokens) }
                if (best != null && score(best, tokens) > 0)
                    textResult("# ${best.title}   (matched ${best.cat}/${best.id})\n\n${best.body}", toolId)
                else textResult("no match for '$q'", toolId)
            }
            "kb_list" -> {
                val cat = args.optString("category").ifBlank { args.optString("cat") }
                val items = if (cat.isBlank()) entries else entries.filter { it.cat == cat }
                if (items.isEmpty() && cat.isNotBlank()) {
                    textResult("unknown category '$cat'. known:\n" +
                        labels.entries.joinToString("\n") { "${it.key}  ${it.value}" }, toolId)
                } else textResult(items.joinToString("\n") { short(it) }, toolId)
            }
            "kb_cats" -> textResult(labels.entries.joinToString("\n") {
                "%-18s %-20s %d entries".format(it.key, it.value, entries.count { e -> e.cat == it.key })
            }, toolId)
            else -> textResult("unknown kb tool: $toolName", toolId)
        }
    }

    /** Tool definitions merged into AgentTools.makeAgentTools. Always exposed
     *  when this build ships the pojia-kb payload (data lives in assets, so it
     *  is available even before the skill is installed to /var/minis). */
    fun toolDefs(): List<com.openminis.app.data.model.AgentToolDefinition> = listOf(
        com.openminis.app.data.model.AgentToolDefinition(
            name = "kb_search",
            description = "Search the local PoJia knowledge base (108 CVE/attack entries: cloud-container, edge-device, exchange-ad, java-deser, java-web, linux-priv, triage, web-cms). Returns scored one-line hits; follow up with kb_read on an id for the full body.",
            parameters = mapOf("query" to com.openminis.app.data.model.AgentToolParam("string", "Free-text query, Chinese or English. e.g. 'nacos', 'log4j', 'sudo 提权'.")),
            required = listOf("query"),
        ),
        com.openminis.app.data.model.AgentToolDefinition(
            name = "kb_read",
            description = "Read one PoJia knowledge base entry in full by id (e.g. 'java-web/cve-2021-29441-nacos' or just 'cve-2021-29441-nacos'). Falls back to best keyword match when the id is not exact.",
            parameters = mapOf("id" to com.openminis.app.data.model.AgentToolParam("string", "Entry id, optionally prefixed with category.")),
            required = listOf("id"),
        ),
        com.openminis.app.data.model.AgentToolDefinition(
            name = "kb_list",
            description = "List PoJia knowledge base entries, optionally filtered by category (cloud-container, edge-device, exchange-ad, java-deser, java-web, linux-priv, triage, web-cms). Omit category to list all 108.",
            parameters = mapOf("category" to com.openminis.app.data.model.AgentToolParam("string", "Optional category filter.")),
        ),
        com.openminis.app.data.model.AgentToolDefinition(
            name = "kb_cats",
            description = "List the eight PoJia knowledge base categories with Chinese labels and entry counts.",
            parameters = emptyMap(),
        ),
    )
}

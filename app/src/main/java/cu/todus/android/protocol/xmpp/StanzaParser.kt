package cu.todus.android.protocol.xmpp

/**
 * Lightweight XMPP stanza representation. We do NOT use a real XML DOM — the toDus dialect uses
 * abbreviated namespaces and attribute names, and the parser must accept both the abbreviated
 * form (`o=`, `i=`, `t=`) and the legacy form (`to=`, `id=`, `type=`).
 */
data class Stanza(
    val name: String,
    val attributes: Map<String, String>,
    val children: List<Stanza>,
    val text: String?,
    val rawXml: String,
) {
    fun attr(name: String): String? = attributes[name]
    fun attrOrDefault(name: String, default: String): String = attributes[name] ?: default
    fun child(name: String): Stanza? = children.firstOrNull { it.name == name }
    fun children(name: String): List<Stanza> = children.filter { it.name == name }
    fun hasChild(name: String): Boolean = children.any { it.name == name }
}

/**
 * Streaming XMPP parser. Operates on a growing buffer; returns parsed top-level stanzas and the
 * index where parsing stopped (so the caller can keep the remainder).
 *
 * Rules implemented per guía.txt §15.1:
 *  - Accept both `o=`/`to=`, `i=`/`id=`, `t=`/`type=`.
 *  - Support self-closing stanzas `<m .../>`.
 *  - Support nested `<b>` content with text.
 *  - Stream open tag `<stream:stream>` is parsed as a normal stanza.
 *  - `</stream:stream>` and unclosed trailing text are tolerated.
 */
internal object StanzaParser {

    /**
     * Parse as many complete top-level stanzas as possible from [buffer].
     * Returns the stanzas and the byte offset where parsing stopped (continuing from there next call).
     */
    fun parse(buffer: String): Pair<List<Stanza>, Int> {
        val out = mutableListOf<Stanza>()
        var i = 0
        val n = buffer.length
        while (i < n) {
            // Skip whitespace and stray text between stanzas.
            while (i < n && buffer[i].isWhitespace()) i++
            if (i >= n) break
            // Skip XML declaration or comments.
            if (buffer.startsWith("<?", i)) {
                val end = buffer.indexOf("?>", i)
                if (end < 0) break
                i = end + 2
                continue
            }
            if (buffer.startsWith("<!--", i)) {
                val end = buffer.indexOf("-->", i)
                if (end < 0) break
                i = end + 3
                continue
            }
            if (buffer[i] != '<') {
                // Stray text — skip up to next '<'
                i = buffer.indexOf('<', i).let { if (it < 0) n else it }
                continue
            }
            // End-of-stream tag.
            if (buffer.startsWith("</stream:stream", i)) {
                val end = buffer.indexOf('>', i)
                if (end < 0) break
                i = end + 1
                continue
            }
            // Closing tag without matching open — defensive skip.
            if (buffer.startsWith("</", i)) {
                val end = buffer.indexOf('>', i)
                if (end < 0) break
                i = end + 1
                continue
            }
            // Try to parse a single element starting at i.
            val parsed = parseElement(buffer, i) ?: break
            out.add(parsed.stanza)
            i = parsed.next
        }
        return out to i
    }

    private data class Parsed(val stanza: Stanza, val next: Int)

    private fun parseElement(s: String, start: Int): Parsed? {
        var i = start + 1
        val n = s.length
        if (i < n && s[i] == '/') return null  // closing tag
        // Read element name (may contain ':')
        val nameStart = i
        while (i < n && !s[i].isWhitespace() && s[i] != '/' && s[i] != '>' && s[i] != '<') i++
        if (i == nameStart) return null
        val name = s.substring(nameStart, i)

        val attrs = LinkedHashMap<String, String>()
        val children = mutableListOf<Stanza>()
        var text: String? = null

        // Parse attributes
        while (i < n) {
            while (i < n && s[i].isWhitespace()) i++
            if (i >= n) return null
            if (s[i] == '/') {
                if (i + 1 < n && s[i + 1] == '>') {
                    val raw = s.substring(start, i + 2)
                    return Parsed(Stanza(name, attrs, children, text, raw), i + 2)
                }
                return null
            }
            if (s[i] == '>') {
                i++
                break
            }
            // attr-name = "value"
            val aNameStart = i
            while (i < n && s[i] != '=' && !s[i].isWhitespace() && s[i] != '/' && s[i] != '>') i++
            val aName = s.substring(aNameStart, i)
            if (i >= n || s[i] != '=') {
                // attribute without value — skip
                if (aName.isNotEmpty()) attrs[aName] = ""
                continue
            }
            i++ // skip '='
            if (i >= n || (s[i] != '"' && s[i] != '\'')) return null
            val quote = s[i]
            i++
            val valueStart = i
            while (i < n && s[i] != quote) {
                if (s[i] == '&') {
                    // entity reference — find ';'
                    val semi = s.indexOf(';', i)
                    if (semi < 0 || semi > i + 8) { i++; continue }
                    i = semi + 1
                } else i++
            }
            if (i >= n) return null
            val rawValue = s.substring(valueStart, i)
            i++ // skip closing quote
            attrs[aName] = unescape(rawValue)
        }

        // Self-closing already returned above. Otherwise we expect children / text.
        // Stream:stream is "open" — treat it as self-closing at open tag.
        if (name == "stream:stream") {
            val raw = s.substring(start, i)
            return Parsed(Stanza(name, attrs, children, text, raw), i)
        }

        // Parse children and text until matching </name>
        val textBuilder = StringBuilder()
        while (i < n) {
            // Find next '<'
            val nextLt = s.indexOf('<', i)
            if (nextLt < 0) {
                textBuilder.append(s.substring(i))
                i = n
                break
            }
            if (nextLt > i) {
                textBuilder.append(s.substring(i, nextLt))
            }
            i = nextLt
            if (s.startsWith("</$name", i) && (i + name.length + 2 < n) && (s[i + name.length + 2] == '>' || s[i + name.length + 2].isWhitespace())) {
                val end = s.indexOf('>', i)
                if (end < 0) return null
                val raw = s.substring(start, end + 1)
                val rawText = textBuilder.toString().trim()
                if (rawText.isNotEmpty()) text = unescape(rawText)
                return Parsed(Stanza(name, attrs, children, text, raw), end + 1)
            }
            if (s.startsWith("</", i)) {
                // different closing tag inside our text — just consume up to '>'
                val end = s.indexOf('>', i)
                if (end < 0) return null
                i = end + 1
                continue
            }
            // Child element
            val child = parseElement(s, i) ?: {
                i++
                null
            }()
            if (child != null) {
                children.add(child.stanza)
                i = child.next
                textBuilder.clear()
            } else {
                i++
            }
        }
        // Reached EOF before closing tag — incomplete.
        return null
    }

    private val namedEntities = mapOf(
        "lt" to "<", "gt" to ">", "amp" to "&", "quot" to "\"", "apos" to "'"
    )

    fun unescape(s: String): String {
        if ('&' !in s) return s
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            if (s[i] == '&') {
                val semi = s.indexOf(';', i)
                if (semi in (i + 1)..(i + 8)) {
                    val ent = s.substring(i + 1, semi)
                    when {
                        ent.startsWith("#x") || ent.startsWith("#X") -> {
                            val code = ent.substring(2).toIntOrNull(16)
                            if (code != null) sb.appendCodePoint(code) else sb.append("&$ent;")
                        }
                        ent.startsWith("#") -> {
                            val code = ent.substring(1).toIntOrNull()
                            if (code != null) sb.appendCodePoint(code) else sb.append("&$ent;")
                        }
                        else -> sb.append(namedEntities[ent] ?: "&$ent;")
                    }
                    i = semi + 1
                    continue
                }
            }
            sb.append(s[i]); i++
        }
        return sb.toString()
    }

    fun escape(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}

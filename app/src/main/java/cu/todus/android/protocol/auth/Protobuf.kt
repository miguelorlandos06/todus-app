package cu.todus.android.protocol.auth

import java.io.ByteArrayOutputStream

/**
 * Minimal protobuf encoder for the toDus auth API.
 *
 * The auth API (auth.todus.cu) uses hand-rolled protobuf — every field is wire-type 2
 * (length-delimited). Field tags are passed as `(field_number << 3) | 2`.
 *
 * Wire format summary:
 *   varint tag  → (field << 3) | wire_type
 *   wire_type 2 → varint length, then UTF-8 bytes
 *
 * No schema file exists; field numbers come from guía.txt §2.
 */
internal object Pb {

    fun bytes(out: ByteArrayOutputStream, field: Int, value: ByteArray) {
        writeVarint(out, (field shl 3) or 2)
        writeVarint(out, value.size)
        out.write(value)
    }

    fun str(out: ByteArrayOutputStream, field: Int, value: String) {
        bytes(out, field, value.toByteArray(Charsets.UTF_8))
    }

    fun int32(out: ByteArrayOutputStream, field: Int, value: Int) {
        // Even ints are encoded as length-delimited strings in the toDus dialect (e.g. version_code).
        str(out, field, value.toString())
    }

    private fun writeVarint(out: ByteArrayOutputStream, v: Int) {
        var value = v
        while (value and -0x80 != 0) {
            out.write((value and 0x7F) or 0x80)
            value = value ushr 7
        }
        out.write(value)
    }
}

/**
 * Tiny protobuf decoder for the auth responses. We only need to pull a single string field
 * (the JWT in token responses, or the password in register responses).
 */
internal object PbReader {

    /**
     * Parse a length-delimited protobuf message and return a map of field number → bytes.
     */
    fun parse(data: ByteArray): Map<Int, ByteArray> {
        val result = LinkedHashMap<Int, ByteArray>()
        var i = 0
        while (i < data.size) {
            val (tag, ni) = readVarint(data, i)
            i = ni
            val field = tag ushr 3
            val wire = tag and 0x07
            when (wire) {
                2 -> {
                    val (len, nl) = readVarint(data, i)
                    i = nl
                    val end = i + len
                    if (end > data.size) break
                    result[field] = data.copyOfRange(i, end)
                    i = end
                }
                0 -> { // varint — skip
                    val (_, nv) = readVarint(data, i)
                    i = nv
                }
                5 -> i += 4 // fixed32
                1 -> i += 8 // fixed64
                else -> break
            }
        }
        return result
    }

    private fun readVarint(data: ByteArray, start: Int): Pair<Int, Int> {
        var value = 0
        var shift = 0
        var i = start
        while (i < data.size) {
            val b = data[i].toInt() and 0xFF
            value = value or ((b and 0x7F) shl shift)
            i++
            if ((b and 0x80) == 0) break
            shift += 7
            if (shift >= 32) break
        }
        return value to i
    }
}

/** A single protobuf message (variable field set). */
internal class PbMsg {
    private val out = ByteArrayOutputStream()
    fun str(field: Int, value: String) = apply { Pb.str(out, field, value) }
    fun bytes(field: Int, value: ByteArray) = apply { Pb.bytes(out, field, value) }
    fun int32(field: Int, value: Int) = apply { Pb.int32(out, field, value) }
    fun build(): ByteArray = out.toByteArray()
}

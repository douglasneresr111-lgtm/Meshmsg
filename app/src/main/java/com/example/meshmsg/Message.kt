package com.example.meshmsg

import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Uma mensagem da rede mesh.
 *
 * id      - identificador único; usado para não retransmitir a mesma mensagem
 *           em loop pela rede.
 * senderName - nome de exibição de quem originou a mensagem (não muda nos saltos).
 * text    - conteúdo da mensagem.
 * ttl     - "time to live": quantos saltos (celular a celular) a mensagem ainda
 *           pode dar antes de parar de ser retransmitida. Cada nó que repassa
 *           decrementa o valor em 1.
 * timestamp - momento de criação, em millis.
 */
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val senderName: String,
    val text: String,
    val ttl: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toBytes(): ByteArray {
        val json = JSONObject()
        json.put("id", id)
        json.put("senderName", senderName)
        json.put("text", text)
        json.put("ttl", ttl)
        json.put("timestamp", timestamp)
        return json.toString().toByteArray(StandardCharsets.UTF_8)
    }

    /** Retorna uma cópia com o TTL reduzido em 1, para repassar ao próximo salto. */
    fun withDecrementedTtl(): Message = copy(ttl = ttl - 1)

    companion object {
        fun fromBytes(bytes: ByteArray): Message? {
            return try {
                val json = JSONObject(String(bytes, StandardCharsets.UTF_8))
                Message(
                    id = json.getString("id"),
                    senderName = json.getString("senderName"),
                    text = json.getString("text"),
                    ttl = json.getInt("ttl"),
                    timestamp = json.getLong("timestamp")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

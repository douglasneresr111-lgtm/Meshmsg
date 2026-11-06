package com.example.meshmsg

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

/**
 * Cuida de toda a parte "mesh": anunciar este aparelho, descobrir vizinhos,
 * conectar com todos que aparecerem por perto, e repassar mensagens de um
 * vizinho para os outros até o TTL (contador de saltos) zerar.
 *
 * P2P_CLUSTER permite que cada aparelho mantenha várias conexões simultâneas
 * (não é 1 para 1), o que é o que forma a malha (mesh): a mensagem entra por
 * um vizinho e sai para todos os outros vizinhos conectados.
 */
class MeshManager(
    private val context: Context,
    private val deviceName: String,
    private val onMessageReceived: (Message) -> Unit,
    private val onPeerCountChanged: (Int) -> Unit
) {
    companion object {
        // Precisa ser igual em todos os aparelhos para eles se encontrarem.
        private const val SERVICE_ID = "com.example.meshmsg.SERVICE"
        private const val TAG = "MeshManager"

        // Quantas mensagens recentes guardar na memória para evitar reenvio em loop.
        private const val SEEN_CACHE_LIMIT = 500

        // Quantos saltos (celular a celular) uma mensagem nova pode dar.
        const val DEFAULT_TTL = 6
    }

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)

    // endpointId -> conectado com sucesso
    private val connectedEndpoints = mutableSetOf<String>()

    // IDs de mensagens já vistas, para não retransmitir em loop pela rede.
    private val seenMessageIds = LinkedHashSet<String>()

    fun start() {
        startAdvertising()
        startDiscovery()
    }

    fun stop() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpoints.clear()
        onPeerCountChanged(0)
    }

    /** Envia uma mensagem nova, criada por este aparelho, para todos os vizinhos conectados. */
    fun sendMessage(text: String) {
        val message = Message(senderName = deviceName, text = text, ttl = DEFAULT_TTL)
        seenMessageIds.add(message.id)
        broadcast(message)
        // Mostra a própria mensagem na tela de quem enviou.
        onMessageReceived(message)
    }

    private fun broadcast(message: Message, excludeEndpointId: String? = null) {
        if (connectedEndpoints.isEmpty()) return
        val payload = Payload.fromBytes(message.toBytes())
        val targets = connectedEndpoints.filter { it != excludeEndpointId }
        if (targets.isNotEmpty()) {
            connectionsClient.sendPayload(targets, payload)
        }
    }

    private fun rememberSeen(id: String) {
        seenMessageIds.add(id)
        if (seenMessageIds.size > SEEN_CACHE_LIMIT) {
            val oldest = seenMessageIds.iterator().next()
            seenMessageIds.remove(oldest)
        }
    }

    // ---------- Anunciar este aparelho para que outros o encontrem ----------

    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startAdvertising(
            deviceName, SERVICE_ID, connectionLifecycleCallback, options
        ).addOnFailureListener { e ->
            Log.e(TAG, "Falha ao anunciar: ${e.message}")
        }
    }

    // ---------- Procurar outros aparelhos por perto ----------

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startDiscovery(
            SERVICE_ID, endpointDiscoveryCallback, options
        ).addOnFailureListener { e ->
            Log.e(TAG, "Falha ao descobrir: ${e.message}")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            connectionsClient.requestConnection(deviceName, endpointId, connectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {
            // Vizinho saiu de alcance antes mesmo de conectar; nada a fazer aqui.
        }
    }

    // ---------- Ciclo de vida da conexão com cada vizinho ----------

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Aceita automaticamente qualquer pedido de conexão.
            // Para produção, aqui seria o lugar de mostrar um código de
            // verificação (info.authenticationDigits) e pedir confirmação do usuário.
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                connectedEndpoints.add(endpointId)
                onPeerCountChanged(connectedEndpoints.size)
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            onPeerCountChanged(connectedEndpoints.size)
        }
    }

    // ---------- Recebimento e retransmissão de mensagens ----------

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            val message = Message.fromBytes(bytes) ?: return

            // Já vimos essa mensagem antes (veio por outro caminho da malha)? Ignora.
            if (seenMessageIds.contains(message.id)) return
            rememberSeen(message.id)

            onMessageReceived(message)

            // Repassa para os outros vizinhos, menos quem acabou de mandar,
            // e só se ainda sobrar salto (TTL).
            if (message.ttl > 0) {
                broadcast(message.withDecrementedTtl(), excludeEndpointId = endpointId)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Não precisamos tratar progresso de transferência para mensagens de texto curtas.
        }
    }
}

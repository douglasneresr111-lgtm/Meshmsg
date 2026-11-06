package com.example.meshmsg

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meshmsg.databinding.ActivityMainBinding
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var meshManager: MeshManager
    private lateinit var adapter: MessageAdapter

    // Nome de exibição simples, gerado uma vez por instalação.
    private val deviceName: String by lazy {
        "User-" + UUID.randomUUID().toString().take(4)
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {
                meshManager.start()
            } else {
                Toast.makeText(
                    this,
                    "Sem essas permissões o app não consegue achar aparelhos por perto.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = MessageAdapter(mutableListOf())
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter

        meshManager = MeshManager(
            context = applicationContext,
            deviceName = deviceName,
            onMessageReceived = { message ->
                runOnUiThread {
                    adapter.addMessage(message)
                    binding.rvMessages.scrollToPosition(adapter.itemCount - 1)
                }
            },
            onPeerCountChanged = { count ->
                runOnUiThread {
                    binding.tvPeers.text = "$count pessoa(s) próxima(s) conectada(s)"
                }
            }
        )

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                meshManager.sendMessage(text)
                binding.etMessage.text.clear()
            }
        }

        requestNeededPermissions()
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        requestPermissionsLauncher.launch(permissions.toTypedArray())
    }

    override fun onDestroy() {
        super.onDestroy()
        meshManager.stop()
    }
}

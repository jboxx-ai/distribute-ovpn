package fyi.rifqifardi.vpn.android

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import fyi.rifqifardi.vpn.android.service.AndroidTunnelController
import fyi.rifqifardi.vpn.android.service.VpnTunnelService
import fyi.rifqifardi.vpn.model.TunnelState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : Activity() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "KMP VPN Client (MVP)"
            textSize = 22f
        }
        layout.addView(title)

        statusText = TextView(this).apply {
            text = "Status: DISCONNECTED"
            textSize = 16f
            setPadding(0, 16, 0, 16)
        }
        layout.addView(statusText)

        val connectBtn = Button(this).apply {
            text = "Connect VPN"
            setOnClickListener {
                startVpn()
            }
        }
        layout.addView(connectBtn)

        val disconnectBtn = Button(this).apply {
            text = "Disconnect VPN"
            setOnClickListener {
                stopVpn()
            }
        }
        layout.addView(disconnectBtn)

        setContentView(layout)

        scope.launch {
            AndroidTunnelController.state.collect { state ->
                statusText.text = "Status: $state"
            }
        }
    }

    private fun startVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null)
        }
    }

    private fun stopVpn() {
        val intent = Intent(this, VpnTunnelService::class.java).apply {
            action = VpnTunnelService.ACTION_DISCONNECT
        }
        startService(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            val intent = Intent(this, VpnTunnelService::class.java).apply {
                action = VpnTunnelService.ACTION_CONNECT
            }
            startService(intent)
        }
    }

    companion object {
        private const val VPN_REQUEST_CODE = 1001
    }
}

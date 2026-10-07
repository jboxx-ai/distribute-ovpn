package fyi.rifqifardi.vpn.android.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import fyi.rifqifardi.vpn.domain.contracts.TunnelController
import fyi.rifqifardi.vpn.domain.contracts.TunnelResult
import fyi.rifqifardi.vpn.domain.contracts.ValidatedProfile
import fyi.rifqifardi.vpn.model.TunnelState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VpnTunnelService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "VpnTunnelService onStartCommand action=${intent?.action}")
        when (intent?.action) {
            ACTION_CONNECT -> establishTunnel()
            ACTION_DISCONNECT -> stopTunnel()
        }
        return START_NOT_STICKY
    }

    private fun establishTunnel() {
        try {
            val builder = Builder()
                .setSession("KmpVpnTunnel")
                .addAddress("10.8.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")

            vpnInterface = builder.establish()
            AndroidTunnelController.updateState(TunnelState.CONNECTED)
            Log.i(TAG, "VpnTunnelService interface established successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish VPN interface", e)
            AndroidTunnelController.updateState(TunnelState.FAILED)
        }
    }

    private fun stopTunnel() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            AndroidTunnelController.updateState(TunnelState.DISCONNECTED)
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping VPN interface", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTunnel()
    }

    companion object {
        const val TAG = "VpnTunnelService"
        const val ACTION_CONNECT = "fyi.rifqifardi.vpn.action.CONNECT"
        const val ACTION_DISCONNECT = "fyi.rifqifardi.vpn.action.DISCONNECT"
    }
}

object AndroidTunnelController : TunnelController {
    private val _state = MutableStateFlow(TunnelState.DISCONNECTED)
    override val state: StateFlow<TunnelState> = _state.asStateFlow()

    fun updateState(newState: TunnelState) {
        _state.value = newState
    }

    override suspend fun prepare(profile: ValidatedProfile): TunnelResult {
        return TunnelResult.Success
    }

    override suspend fun connect(): TunnelResult {
        _state.value = TunnelState.CONNECTING
        return TunnelResult.Success
    }

    override suspend fun disconnect(reason: String): TunnelResult {
        _state.value = TunnelState.DISCONNECTING
        _state.value = TunnelState.DISCONNECTED
        return TunnelResult.Success
    }
}

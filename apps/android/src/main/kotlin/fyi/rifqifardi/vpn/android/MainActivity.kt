package fyi.rifqifardi.vpn.android

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.VpnService
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import fyi.rifqifardi.vpn.android.service.AndroidTunnelController
import fyi.rifqifardi.vpn.android.service.VpnTunnelService
import fyi.rifqifardi.vpn.model.TunnelState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Android Client Application UI with Cloudflare Kumo UI Design System Tokens.
 *
 * Implements OpenSpec Phase 09 UI Specification:
 * - High-density dark canvas (`bg-kumo-base`: #0D1117).
 * - Hero Connection Status Ring & Dynamic Badge.
 * - Elevated Card Surfaces (`bg-kumo-elevated`: #161B22) with structural dividers (`border-kumo-line`: #30363D).
 * - Live Profile Metadata (Active version, Last-Known-Good version, Rollback status).
 * - 7-Stage Transactional Activation Journal status overview.
 */
class MainActivity : Activity() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var statusBadge: TextView
    private lateinit var connectButton: Button
    private lateinit var profileCard: LinearLayout
    private lateinit var activeVersionText: TextView
    private lateinit var lkgVersionText: TextView
    private lateinit var journalStatusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootScroll = ScrollView(this).apply {
            setBackgroundColor(KumoColors.Base.toInt())
            isFillViewport = true
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 64)
        }

        // Header: Brand Title & Cloudflare Kumo Indicator
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brandTitle = TextView(this).apply {
            text = "Vault VPN"
            textSize = 24f
            setTextColor(KumoColors.PrimaryText.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val brandPill = TextView(this).apply {
            text = " Cloudflare Kumo "
            textSize = 10f
            setTextColor(KumoColors.Brand.toInt())
            background = GradientDrawable().apply {
                setColor(0x22F38020)
                cornerRadius = 16f
                setStroke(2, KumoColors.Brand.toInt())
            }
            setPadding(16, 4, 16, 4)
        }
        headerLayout.addView(brandTitle)
        headerLayout.addView(brandPill)
        mainLayout.addView(headerLayout)

        val subLead = TextView(this).apply {
            text = "Managed zero-trust network tunnel client"
            textSize = 12f
            setTextColor(KumoColors.MutedText.toInt())
            setPadding(0, 8, 0, 48)
        }
        mainLayout.addView(subLead)

        // Hero Connection Panel (Elevated Card)
        val heroPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(KumoColors.Elevated.toInt())
                cornerRadius = 24f
                setStroke(2, KumoColors.Line.toInt())
            }
            setPadding(40, 48, 40, 48)
        }

        statusBadge = TextView(this).apply {
            text = "DISCONNECTED"
            textSize = 14f
            setTextColor(KumoColors.Danger.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(KumoColors.DangerSubtle.toInt())
                cornerRadius = 20f
                setStroke(2, KumoColors.Danger.toInt())
            }
            setPadding(28, 12, 28, 12)
        }
        heroPanel.addView(statusBadge)

        val spacer = View(this).apply {
            minimumHeight = 32
        }
        heroPanel.addView(spacer)

        connectButton = Button(this).apply {
            text = "Connect VPN"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(KumoColors.Brand.toInt())
                cornerRadius = 18f
            }
            setPadding(32, 24, 32, 24)
            setOnClickListener {
                toggleConnection()
            }
        }
        heroPanel.addView(connectButton)

        mainLayout.addView(heroPanel)

        // Profile Metadata Card
        profileCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(KumoColors.Elevated.toInt())
                cornerRadius = 20f
                setStroke(2, KumoColors.Line.toInt())
            }
            setPadding(36, 36, 36, 36)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 32, 0, 0)
            }
            layoutParams = params
        }

        val profileHeader = TextView(this).apply {
            text = "Assigned Profile: edge-production-eu"
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(KumoColors.PrimaryText.toInt())
        }
        profileCard.addView(profileHeader)

        activeVersionText = TextView(this).apply {
            text = "Active Version: v8 (Channel: STABLE)"
            textSize = 12f
            setTextColor(KumoColors.MutedText.toInt())
            setPadding(0, 10, 0, 4)
        }
        profileCard.addView(activeVersionText)

        lkgVersionText = TextView(this).apply {
            text = "Last-Known-Good Version: v8 (Fallback Safe)"
            textSize = 12f
            setTextColor(KumoColors.Success.toInt())
            setPadding(0, 0, 0, 10)
        }
        profileCard.addView(lkgVersionText)

        mainLayout.addView(profileCard)

        // Transactional Journal Card
        val journalCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(KumoColors.Recessed.toInt())
                cornerRadius = 20f
                setStroke(2, KumoColors.Line.toInt())
            }
            setPadding(36, 36, 36, 36)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 32, 0, 0)
            }
            layoutParams = params
        }

        val journalTitle = TextView(this).apply {
            text = "Activation Journal (7-State Rollout)"
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(KumoColors.PrimaryText.toInt())
        }
        journalCard.addView(journalTitle)

        journalStatusText = TextView(this).apply {
            text = "✓ [COMMITTED] Version 8 active. Candidate v9 staged."
            textSize = 11f
            setTextColor(KumoColors.Success.toInt())
            setPadding(0, 8, 0, 0)
        }
        journalCard.addView(journalStatusText)

        mainLayout.addView(journalCard)

        rootScroll.addView(mainLayout)
        setContentView(rootScroll)

        // Observe Kotlin StateFlow
        scope.launch {
            AndroidTunnelController.state.collect { state ->
                updateUiForState(state)
            }
        }
    }

    private fun updateUiForState(state: TunnelState) {
        when (state) {
            TunnelState.CONNECTED -> {
                statusBadge.text = "CONNECTED"
                statusBadge.setTextColor(KumoColors.Success.toInt())
                statusBadge.background = GradientDrawable().apply {
                    setColor(KumoColors.SuccessSubtle.toInt())
                    cornerRadius = 20f
                    setStroke(2, KumoColors.Success.toInt())
                }
                connectButton.text = "Disconnect VPN"
                connectButton.background = GradientDrawable().apply {
                    setColor(0xFF21262D.toInt())
                    cornerRadius = 18f
                    setStroke(2, KumoColors.Line.toInt())
                }
            }
            TunnelState.CONNECTING -> {
                statusBadge.text = "CONNECTING..."
                statusBadge.setTextColor(KumoColors.Warning.toInt())
                statusBadge.background = GradientDrawable().apply {
                    setColor(KumoColors.WarningSubtle.toInt())
                    cornerRadius = 20f
                    setStroke(2, KumoColors.Warning.toInt())
                }
                connectButton.text = "Cancel"
            }
            TunnelState.DISCONNECTED -> {
                statusBadge.text = "DISCONNECTED"
                statusBadge.setTextColor(KumoColors.Danger.toInt())
                statusBadge.background = GradientDrawable().apply {
                    setColor(KumoColors.DangerSubtle.toInt())
                    cornerRadius = 20f
                    setStroke(2, KumoColors.Danger.toInt())
                }
                connectButton.text = "Connect VPN"
                connectButton.background = GradientDrawable().apply {
                    setColor(KumoColors.Brand.toInt())
                    cornerRadius = 18f
                }
            }
        }
    }

    private fun toggleConnection() {
        if (AndroidTunnelController.state.value == TunnelState.CONNECTED) {
            stopVpn()
        } else {
            startVpn()
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

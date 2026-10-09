package fyi.rifqifardi.vpn.android

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Insets
import android.graphics.drawable.GradientDrawable
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
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
 *
 * WP3-prime: all setPadding/setMargins/cornerRadius/setStroke/minimumHeight values are now
 * expressed in dp via [KumoDimens] so they render at correct physical size on any density.
 * WindowInsets are consumed to prevent edge-to-edge occlusion under the system bars.
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
            // Outer gutters: 24dp horizontal, 24dp vertical base.
            // Insets will be added on top of these after layout.
            padLtrb(this@MainActivity, KumoDimens.GutterOuter, KumoDimens.PaddingVerticalBase,
                     KumoDimens.GutterOuter, KumoDimens.PaddingVerticalBase)
        }

        // ── Header: Brand Title & Cloudflare Kumo Indicator ──────────────────────────────
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brandTitle = TextView(this).apply {
            text = "VPN Distribution"
            textSize = 24f   // SP — not a density bug; leave as-is
            setTextColor(KumoColors.PrimaryText.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val brandPill = TextView(this).apply {
            text = " Cloudflare Kumo "
            textSize = 10f   // SP
            setTextColor(KumoColors.Brand.toInt())
            background = kumoSurface(
                context = this@MainActivity,
                fillColor = 0x22F38020,
                strokeColor = KumoColors.Brand.toInt(),
                fullyRounded = true,
            )
            padLtrb(this@MainActivity, KumoDimens.BrandPillPadH, KumoDimens.BrandPillPadV,
                     KumoDimens.BrandPillPadH, KumoDimens.BrandPillPadV)
        }
        headerLayout.addView(brandTitle)
        headerLayout.addView(brandPill)
        mainLayout.addView(headerLayout)

        val subLead = TextView(this).apply {
            text = "Managed zero-trust network tunnel client"
            textSize = 12f   // SP
            setTextColor(KumoColors.MutedText.toInt())
            padVertical(this@MainActivity, KumoDimens.MetaLineGap, KumoDimens.HeaderBottomGap)
        }
        mainLayout.addView(subLead)

        // ── Hero Connection Panel (Elevated Card) ────────────────────────────────────────
        val heroPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = kumoSurface(
                context = this@MainActivity,
                fillColor = KumoColors.Elevated.toInt(),
                strokeColor = KumoColors.Line.toInt(),
                cornerRadiusDp = KumoDimens.RadiusCard,
            )
            padLtrb(this@MainActivity, KumoDimens.HeroPaddingH, KumoDimens.HeroPaddingV,
                     KumoDimens.HeroPaddingH, KumoDimens.HeroPaddingV)
            // FIX: explicit MATCH_PARENT/WRAP_CONTENT + top margin like sibling cards.
            layoutParams = matchWrapWithTopMargin(this@MainActivity, KumoDimens.CardGap)
        }

        statusBadge = TextView(this).apply {
            text = "DISCONNECTED"
            textSize = 14f   // SP
            setTextColor(KumoColors.Danger.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = kumoSurface(
                context = this@MainActivity,
                fillColor = KumoColors.DangerSubtle.toInt(),
                strokeColor = KumoColors.Danger.toInt(),
                fullyRounded = true,
            )
            padLtrb(this@MainActivity, KumoDimens.BadgePadH, KumoDimens.BadgePadV,
                     KumoDimens.BadgePadH, KumoDimens.BadgePadV)
        }
        heroPanel.addView(statusBadge)

        val spacer = View(this).apply {
            minimumHeight = KumoDimens.SpacerHeroToButton.dpInt(this@MainActivity)
        }
        heroPanel.addView(spacer)

        connectButton = Button(this).apply {
            text = "Connect VPN"
            setTextColor(Color.WHITE)
            textSize = 15f   // SP
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = kumoSurface(
                context = this@MainActivity,
                fillColor = KumoColors.Brand.toInt(),
                cornerRadiusDp = KumoDimens.RadiusButton,
            )
            padLtrb(this@MainActivity, KumoDimens.ButtonPadH, 0f, KumoDimens.ButtonPadH, 0f)
            minimumHeight = KumoDimens.ButtonMinHeight.dpInt(this@MainActivity)
            // Full-width within the hero panel.
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            setOnClickListener { toggleConnection() }
        }
        heroPanel.addView(connectButton)

        mainLayout.addView(heroPanel)

        // ── Profile Metadata Card ────────────────────────────────────────────────────────
        profileCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = kumoSurface(
                context = this@MainActivity,
                fillColor = KumoColors.Elevated.toInt(),
                strokeColor = KumoColors.Line.toInt(),
                cornerRadiusDp = KumoDimens.RadiusCard,
            )
            padAll(this@MainActivity, KumoDimens.CardPadding)
            layoutParams = matchWrapWithTopMargin(this@MainActivity, KumoDimens.CardGap)
        }

        val profileHeader = TextView(this).apply {
            text = "Assigned Profile: edge-production-eu"
            textSize = 14f   // SP
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(KumoColors.PrimaryText.toInt())
        }
        profileCard.addView(profileHeader)

        activeVersionText = TextView(this).apply {
            text = "Active Version: v8 (Channel: STABLE)"
            textSize = 12f   // SP
            setTextColor(KumoColors.MutedText.toInt())
            padVertical(this@MainActivity, KumoDimens.MetaLineGap, 0f)
        }
        profileCard.addView(activeVersionText)

        lkgVersionText = TextView(this).apply {
            text = "Last-Known-Good Version: v8 (Fallback Safe)"
            textSize = 12f   // SP
            setTextColor(KumoColors.Success.toInt())
            padVertical(this@MainActivity, 0f, KumoDimens.MetaLineGap)
        }
        profileCard.addView(lkgVersionText)

        mainLayout.addView(profileCard)

        // ── Transactional Journal Card ───────────────────────────────────────────────────
        val journalCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = kumoSurface(
                context = this@MainActivity,
                fillColor = KumoColors.Recessed.toInt(),
                strokeColor = KumoColors.Line.toInt(),
                cornerRadiusDp = KumoDimens.RadiusCard,
            )
            padAll(this@MainActivity, KumoDimens.CardPadding)
            layoutParams = matchWrapWithTopMargin(this@MainActivity, KumoDimens.CardGap)
        }

        val journalTitle = TextView(this).apply {
            text = "Activation Journal (7-State Rollout)"
            textSize = 13f   // SP
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(KumoColors.PrimaryText.toInt())
        }
        journalCard.addView(journalTitle)

        journalStatusText = TextView(this).apply {
            text = "✓ [COMMITTED] Version 8 active. Candidate v9 staged."
            textSize = 11f   // SP
            setTextColor(KumoColors.Success.toInt())
            padVertical(this@MainActivity, KumoDimens.MetaLineGap, 0f)
        }
        journalCard.addView(journalStatusText)

        mainLayout.addView(journalCard)

        rootScroll.addView(mainLayout)
        setContentView(rootScroll)

        // ── WindowInsets: consume system bars so content is never occluded ────────────────
        // targetSdk 35 forces edge-to-edge; minSdk 26 means we must branch on API level.
        // No androidx dependency required — pure platform API.
        applySystemBarInsets(rootScroll, mainLayout)

        // ── Capsule radii for pill/badge: applied after first layout pass ─────────────────
        rootScroll.post {
            makeCapsule(brandPill)
            makeCapsule(statusBadge)
        }

        // Observe Kotlin StateFlow
        scope.launch {
            AndroidTunnelController.state.collect { state ->
                updateUiForState(state)
            }
        }
    }

    /**
     * Attach a WindowInsets listener to [rootScroll] that adds system-bar insets to the
     * existing padding of [mainLayout]. Uses platform API only (no androidx.core):
     * - API 30+: WindowInsets.Type.systemBars()
     * - API 26–29: deprecated systemWindowInsetTop/Bottom/Left/Right
     *
     * The base padding (gutters + vertical) is stored separately so repeated inset calls
     * don't accumulate. We recompute from scratch each time.
     */
    private fun applySystemBarInsets(rootScroll: ScrollView, mainLayout: LinearLayout) {
        val baseL = KumoDimens.GutterOuter.dpInt(this)
        val baseT = KumoDimens.PaddingVerticalBase.dpInt(this)
        val baseR = KumoDimens.GutterOuter.dpInt(this)
        val baseB = KumoDimens.PaddingVerticalBase.dpInt(this)

        rootScroll.setOnApplyWindowInsetsListener { _, insets ->
            val sysL: Int
            val sysT: Int
            val sysR: Int
            val sysB: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val barInsets: Insets = insets.getInsets(WindowInsets.Type.systemBars())
                sysL = barInsets.left
                sysT = barInsets.top
                sysR = barInsets.right
                sysB = barInsets.bottom
            } else {
                @Suppress("DEPRECATION")
                sysL = insets.systemWindowInsetLeft
                @Suppress("DEPRECATION")
                sysT = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                sysR = insets.systemWindowInsetRight
                @Suppress("DEPRECATION")
                sysB = insets.systemWindowInsetBottom
            }
            mainLayout.setPadding(baseL + sysL, baseT + sysT, baseR + sysR, baseB + sysB)
            insets
        }
        rootScroll.requestApplyInsets()
    }

    private fun updateUiForState(state: TunnelState) {
        when (state) {
            TunnelState.CONNECTED -> {
                statusBadge.text = "CONNECTED"
                statusBadge.setTextColor(KumoColors.Success.toInt())
                statusBadge.background = kumoSurface(
                    context = this,
                    fillColor = KumoColors.SuccessSubtle.toInt(),
                    strokeColor = KumoColors.Success.toInt(),
                    fullyRounded = true,
                )
                statusBadge.post { makeCapsule(statusBadge) }
                connectButton.text = "Disconnect VPN"
                connectButton.background = kumoSurface(
                    context = this,
                    fillColor = 0xFF21262D.toInt(),
                    strokeColor = KumoColors.Line.toInt(),
                    cornerRadiusDp = KumoDimens.RadiusButton,
                )
            }
            TunnelState.CONNECTING,
            TunnelState.RECONNECTING -> {
                statusBadge.text = if (state == TunnelState.RECONNECTING) "RECONNECTING..." else "CONNECTING..."
                statusBadge.setTextColor(KumoColors.Warning.toInt())
                statusBadge.background = kumoSurface(
                    context = this,
                    fillColor = KumoColors.WarningSubtle.toInt(),
                    strokeColor = KumoColors.Warning.toInt(),
                    fullyRounded = true,
                )
                statusBadge.post { makeCapsule(statusBadge) }
                connectButton.text = "Cancel"
            }
            TunnelState.DISCONNECTING -> {
                statusBadge.text = "DISCONNECTING..."
                statusBadge.setTextColor(KumoColors.Warning.toInt())
                statusBadge.background = kumoSurface(
                    context = this,
                    fillColor = KumoColors.WarningSubtle.toInt(),
                    strokeColor = KumoColors.Warning.toInt(),
                    fullyRounded = true,
                )
                statusBadge.post { makeCapsule(statusBadge) }
                connectButton.text = "Disconnecting..."
            }
            TunnelState.DISCONNECTED,
            TunnelState.FAILED -> {
                statusBadge.text = if (state == TunnelState.FAILED) "CONNECTION FAILED" else "DISCONNECTED"
                statusBadge.setTextColor(KumoColors.Danger.toInt())
                statusBadge.background = kumoSurface(
                    context = this,
                    fillColor = KumoColors.DangerSubtle.toInt(),
                    strokeColor = KumoColors.Danger.toInt(),
                    fullyRounded = true,
                )
                statusBadge.post { makeCapsule(statusBadge) }
                connectButton.text = "Connect VPN"
                connectButton.background = kumoSurface(
                    context = this,
                    fillColor = KumoColors.Brand.toInt(),
                    cornerRadiusDp = KumoDimens.RadiusButton,
                )
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
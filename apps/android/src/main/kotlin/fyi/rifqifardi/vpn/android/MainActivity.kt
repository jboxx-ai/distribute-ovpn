package fyi.rifqifardi.vpn.android

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Insets
import android.graphics.drawable.GradientDrawable
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    // Android's platform main looper is available before coroutine service-loader
    // initialization. Keep activity startup independent of Dispatchers.Main so a
    // broken/missing MainDispatcherFactory cannot abort onCreate.
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var statusBadge: TextView
    private lateinit var connectButton: Button
    private lateinit var profileCard: LinearLayout
    private lateinit var activeVersionText: TextView
    private lateinit var lkgVersionText: TextView
    private lateinit var journalStatusText: TextView
    private lateinit var onboardingPanel: LinearLayout
    private lateinit var onboardingMessage: TextView
    private lateinit var onboardingAction: Button
    private lateinit var onboardingStore: OnboardingStore
    private val registryClient = RegistryClient()
    private var onboardingState = OnboardingState.UNCONFIGURED
    private var onboardingBusy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootScroll = ScrollView(this).apply {
            setBackgroundColor(KumoColors.Base.toInt())
            isFillViewport = true
        }

        onboardingStore = OnboardingStore(this)

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

        onboardingPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = kumoSurface(this@MainActivity, KumoColors.Elevated.toInt(), KumoColors.Line.toInt(), cornerRadiusDp = KumoDimens.RadiusCard)
            padAll(this@MainActivity, KumoDimens.CardPadding)
            layoutParams = matchWrapWithTopMargin(this@MainActivity, KumoDimens.CardGap)
        }
        onboardingMessage = TextView(this).apply {
            setTextColor(KumoColors.PrimaryText.toInt())
            textSize = 14f
        }
        onboardingPanel.addView(onboardingMessage)
        onboardingAction = Button(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            minimumHeight = KumoDimens.ButtonMinHeight.dpInt(this@MainActivity)
            background = kumoSurface(this@MainActivity, KumoColors.Brand.toInt(), cornerRadiusDp = KumoDimens.RadiusButton)
            setOnClickListener { handleOnboardingAction() }
        }
        onboardingPanel.addView(onboardingAction)
        mainLayout.addView(onboardingPanel)

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
        renderOnboarding()

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
                mainHandler.post { updateUiForState(state) }
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

    private fun renderOnboarding() {
        onboardingState = deriveOnboardingState(onboardingStore.registryUrl, onboardingStore.deviceId)
        when (onboardingState) {
            OnboardingState.UNCONFIGURED -> {
                onboardingMessage.text = "Connect this device to your registry. Your administrator provides the HTTPS address and one-time enrollment token."
                onboardingAction.text = "Set up this device"
                onboardingAction.isEnabled = !onboardingBusy
            }
            OnboardingState.NOT_ENROLLED -> {
                onboardingMessage.text = "Registry saved. Enroll this device with the one-time token from your administrator."
                onboardingAction.text = "Enter enrollment token"
                onboardingAction.isEnabled = !onboardingBusy
            }
            OnboardingState.PROFILE_SYNC_REQUIRED -> {
                onboardingMessage.text = "Device enrolled. Profile sync and signature verification are required before VPN connection."
                onboardingAction.text = "Profile sync unavailable"
                onboardingAction.isEnabled = false
            }
            OnboardingState.CHECKING_REGISTRY -> {
                onboardingMessage.text = "Checking registry…"
                onboardingAction.text = "Checking…"
                onboardingAction.isEnabled = false
            }
            OnboardingState.REGISTRY_UNAVAILABLE -> {
                onboardingMessage.text = "Registry unavailable. Check the address and network, then try again."
                onboardingAction.text = "Try again"
                onboardingAction.isEnabled = !onboardingBusy
            }
            OnboardingState.ENROLLING -> {
                onboardingMessage.text = "Enrolling this device…"
                onboardingAction.text = "Enrolling…"
                onboardingAction.isEnabled = false
            }
            OnboardingState.ENROLLMENT_ERROR -> {
                onboardingMessage.text = "Enrollment failed. Check the one-time token or contact your administrator."
                onboardingAction.text = "Try again"
                onboardingAction.isEnabled = !onboardingBusy
            }
            OnboardingState.ENROLLMENT_FAILED -> {
                onboardingMessage.text = "Enrollment is not complete. Profile sync and VPN connection remain unavailable."
                onboardingAction.text = "Review setup"
                onboardingAction.isEnabled = !onboardingBusy
            }
        }
        onboardingPanel.visibility = View.VISIBLE
        // The current service is a TUN shell without OpenVPN packet forwarding.
        // Do not present it as a usable VPN until profile sync, preflight, and data-path evidence exist.
        connectButton.isEnabled = false
        connectButton.alpha = 0.55f
    }

    private fun handleOnboardingAction() {
        if (onboardingBusy) return
        when (onboardingState) {
            OnboardingState.UNCONFIGURED,
            OnboardingState.REGISTRY_UNAVAILABLE,
            OnboardingState.ENROLLMENT_ERROR -> showRegistryDialog()
            OnboardingState.NOT_ENROLLED -> showEnrollmentDialog()
            OnboardingState.PROFILE_SYNC_REQUIRED -> Unit
            else -> Unit
        }
    }

    private fun showRegistryDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "https://registry.example.com"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(onboardingStore.registryUrl.orEmpty())
            isSaveEnabled = false
        }
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Connect to your registry")
            .setMessage("Use the HTTPS address provided by your administrator.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Test connection", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val url = input.text.toString().trim().trimEnd('/')
                if (url.isBlank()) { input.error = "Registry address is required"; return@setOnClickListener }
                onboardingBusy = true
                onboardingState = OnboardingState.CHECKING_REGISTRY
                renderOnboarding()
                scope.launch {
                    val result = registryClient.checkHealth(url)
                    mainHandler.post {
                        onboardingBusy = false
                        if (result.isSuccess) {
                            onboardingStore.registryUrl = url
                            onboardingState = OnboardingState.NOT_ENROLLED
                            dialog.dismiss()
                        } else {
                            onboardingState = OnboardingState.REGISTRY_UNAVAILABLE
                            input.error = "Registry unavailable"
                        }
                        renderOnboarding()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showEnrollmentDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "One-time enrollment token"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            // Prevent Android view-state restoration from retaining the bearer token.
            isSaveEnabled = false
        }
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Enroll this device")
            .setMessage("Paste the single-use token supplied by your administrator. The app will not store it.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Enroll device", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val token = input.text.toString()
                val url = onboardingStore.registryUrl
                if (token.isBlank() || url.isNullOrBlank()) { input.error = "Token and registry are required"; return@setOnClickListener }
                onboardingBusy = true
                onboardingState = OnboardingState.ENROLLING
                renderOnboarding()
                scope.launch {
                    val result = registryClient.enroll(url, token)
                    input.text.clear()
                    mainHandler.post {
                        onboardingBusy = false
                        result.onSuccess { credentials ->
                            onboardingStore.saveEnrollment(credentials.deviceId, credentials.apiKey, credentials.signingPublicKeyBase64)
                            onboardingState = OnboardingState.PROFILE_SYNC_REQUIRED
                            dialog.dismiss()
                        }.onFailure {
                            onboardingState = OnboardingState.ENROLLMENT_ERROR
                        }
                        renderOnboarding()
                    }
                }
            }
        }
        dialog.show()
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
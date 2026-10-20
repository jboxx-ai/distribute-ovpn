package fyi.rifqifardi.vpn.android

/**
 * UI-only onboarding states. This deliberately does not imply a working VPN data path;
 * tunnel readiness is represented separately by the existing TunnelState.
 */
enum class OnboardingState {
    UNCONFIGURED,
    CHECKING_REGISTRY,
    REGISTRY_UNAVAILABLE,
    NOT_ENROLLED,
    ENROLLING,
    ENROLLMENT_ERROR,
    PROFILE_SYNC_REQUIRED,
    ENROLLMENT_FAILED,
}

internal fun deriveOnboardingState(registryUrl: String?, deviceId: String?): OnboardingState = when {
    registryUrl.isNullOrBlank() -> OnboardingState.UNCONFIGURED
    deviceId.isNullOrBlank() -> OnboardingState.NOT_ENROLLED
    else -> OnboardingState.PROFILE_SYNC_REQUIRED // Enrollment exists; sync/verification is the next required step.
}

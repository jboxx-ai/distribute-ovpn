package fyi.rifqifardi.vpn.android

import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingStateTest {
    @Test
    fun missingRegistryIsUnconfigured() {
        assertEquals(OnboardingState.UNCONFIGURED, deriveOnboardingState(null, null))
        assertEquals(OnboardingState.UNCONFIGURED, deriveOnboardingState("", null))
    }

    @Test
    fun configuredRegistryWithoutDeviceNeedsEnrollment() {
        assertEquals(OnboardingState.NOT_ENROLLED, deriveOnboardingState("https://registry.example", null))
    }

    @Test
    fun enrolledDeviceRequiresProfileSyncBeforeVpn() {
        assertEquals(
            OnboardingState.PROFILE_SYNC_REQUIRED,
            deriveOnboardingState("https://registry.example", "device-1")
        )
    }
}

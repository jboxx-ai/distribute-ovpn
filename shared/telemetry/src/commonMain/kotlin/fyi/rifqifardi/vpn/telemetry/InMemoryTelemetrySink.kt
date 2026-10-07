package fyi.rifqifardi.vpn.telemetry

import fyi.rifqifardi.vpn.domain.contracts.TelemetrySink

class InMemoryTelemetrySink : TelemetrySink {
    val recordedEvents = mutableListOf<Pair<String, Map<String, String>>>()
    val recordedErrors = mutableListOf<Pair<String, Throwable?>>()

    override suspend fun recordEvent(name: String, attributes: Map<String, String>) {
        recordedEvents.add(name to attributes)
    }

    override suspend fun recordError(message: String, throwable: Throwable?) {
        recordedErrors.add(message to throwable)
    }

    fun clear() {
        recordedEvents.clear()
        recordedErrors.clear()
    }
}

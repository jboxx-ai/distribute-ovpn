import SwiftUI
import NetworkExtension

@main
struct VpnMacOSApp: App {
    @StateObject private var tunnelManager = MacOSTunnelManager()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(tunnelManager)
                .frame(minWidth: 420, minHeight: 320)
        }
    }
}

import SwiftUI

struct ContentView: View {
    @EnvironmentObject var tunnelManager: MacOSTunnelManager

    var body: some View {
        VStack(spacing: 20) {
            Text("KMP macOS VPN Client")
                .font(.headline)

            VStack(alignment: .leading, spacing: 6) {
                Text("Assigned Profile: \(tunnelManager.assignedProfile)")
                Text("Active Version: \(tunnelManager.activeVersion)")
                Text("Tunnel Status: \(tunnelManager.state.rawValue)")
                    .bold()
                    .foregroundColor(colorForState(tunnelManager.state))
            }
            .padding()
            .background(Color.gray.opacity(0.1))
            .cornerRadius(8)

            HStack(spacing: 16) {
                if tunnelManager.state == .disconnected || tunnelManager.state == .failed {
                    Button("Connect") {
                        tunnelManager.connect()
                    }
                    .buttonStyle(.borderedProminent)
                } else {
                    Button("Disconnect") {
                        tunnelManager.disconnect()
                    }
                    .buttonStyle(.bordered)
                }
            }
        }
        .padding(24)
    }

    private func colorForState(_ state: MacOSTunnelState) -> Color {
        switch state {
        case .connected: return .green
        case .connecting, .disconnecting: return .orange
        case .failed: return .red
        case .disconnected: return .secondary
        }
    }
}

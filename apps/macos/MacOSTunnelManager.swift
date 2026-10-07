import Foundation
import NetworkExtension
import Combine

public enum MacOSTunnelState: String {
    case disconnected = "DISCONNECTED"
    case connecting = "CONNECTING"
    case connected = "CONNECTED"
    case disconnecting = "DISCONNECTING"
    case failed = "FAILED"
}

public class MacOSTunnelManager: ObservableObject {
    @Published public var state: MacOSTunnelState = .disconnected
    @Published public var assignedProfile: String = "corp-vpn"
    @Published public var activeVersion: Int = 1

    private var targetManager: NETunnelProviderManager?

    public init() {
        loadProviderManager()
    }

    public func loadProviderManager() {
        NETunnelProviderManager.loadAllFromPreferences { [weak self] managers, error in
            if let mgr = managers?.first {
                self?.targetManager = mgr
            }
        }
    }

    public func connect() {
        self.state = .connecting
        // Simulate extension handoff / start tunnel
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.state = .connected
        }
    }

    public func disconnect() {
        self.state = .disconnecting
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            self.state = .disconnected
        }
    }
}

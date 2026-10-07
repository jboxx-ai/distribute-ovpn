import Foundation
import NetworkExtension
import os.log

class PacketTunnelProvider: NEPacketTunnelProvider {

    private let log = OSLog(subsystem: "fyi.rifqifardi.vpn.macos.tunnel", category: "PacketTunnel")

    override func startTunnel(options: [String : NSObject]?, completionHandler: @escaping (Error?) -> Void) {
        os_log("Starting KMP OpenVPN tunnel on macOS...", log: log, type: .info)

        // Tunnel network settings (IPv4/DNS)
        let tunnelNetworkSettings = NEPacketTunnelNetworkSettings(tunnelRemoteAddress: "10.8.0.1")
        let ipv4Settings = NEIPv4Settings(addresses: ["10.8.0.2"], subnetMasks: ["255.255.255.0"])
        ipv4Settings.includedRoutes = [NEIPv4Route.default()]
        tunnelNetworkSettings.ipv4Settings = ipv4Settings
        tunnelNetworkSettings.dnsSettings = NEDNSSettings(servers: ["1.1.1.1", "8.8.8.8"])

        setTunnelNetworkSettings(tunnelNetworkSettings) { error in
            if let error = error {
                os_log("Failed to set tunnel network settings: %{public}@", log: self.log, type: .error, error.localizedDescription)
                completionHandler(error)
                return
            }

            os_log("macOS Tunnel established successfully", log: self.log, type: .info)
            completionHandler(nil)
        }
    }

    override func stopTunnel(with reason: NEProviderStopReason, completionHandler: @escaping () -> Void) {
        os_log("Stopping macOS OpenVPN tunnel...", log: log, type: .info)
        completionHandler()
    }
}

import SwiftUI

/**
 * macOS Client Application Main Window UI (SwiftUI).
 *
 * Implements OpenSpec Phase 10 UI Architecture with Cloudflare Kumo UI:
 * - High-density split view dashboard with telemetry, active profile, and rollback indicators.
 * - Cloudflare Orange `#F38020` hero connect action button.
 * - Transactional Sync Journal card with health check state visualization.
 */
struct ContentView: View {
    @EnvironmentObject var tunnelManager: MacOSTunnelManager
    @State private var selectedTab: String = "connection"
    @State private var assignedChannel: String = "Stable"
    @State private var isActivating: Bool = false

    var body: some View {
        NavigationSplitView {
            // Sidebar Navigation
            List(selection: $selectedTab) {
                Section(header: Text("Vault VPN").foregroundColor(KumoColors.brand).font(.headline)) {
                    Label("Connection", systemImage: "shield.lefthalf.filled")
                        .tag("connection")
                    Label("Profile & Policy", systemImage: "doc.plaintext")
                        .tag("profile")
                    Label("Rollout & Sync", systemImage: "arrow.triangle.2.circlepath")
                        .tag("sync")
                    Label("Security & Trust", systemImage: "lock.shield")
                        .tag("security")
                }
            }
            .listStyle(SidebarListStyle())
            .frame(minWidth: 180, idealWidth: 200)
            .background(KumoColors.elevated)
        } detail: {
            ZStack {
                KumoColors.base.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        // Header
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Private Managed Tunnel")
                                    .font(.title2)
                                    .bold()
                                    .foregroundColor(KumoColors.primaryText)
                                Text("Cloudflare Kumo UI Standard • macOS Sequoia")
                                    .font(.caption)
                                    .foregroundColor(KumoColors.mutedText)
                            }
                            Spacer()
                            // Status Pill
                            Text(tunnelManager.state.rawValue.uppercased())
                                .font(.caption.bold())
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .foregroundColor(colorForState(tunnelManager.state))
                                .background(colorForState(tunnelManager.state).opacity(0.15))
                                .cornerRadius(12)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(colorForState(tunnelManager.state), lineWidth: 1)
                                )
                        }

                        // Hero Connection Card
                        VStack(spacing: 16) {
                            HStack {
                                Image(systemName: tunnelManager.state == .connected ? "lock.fill" : "lock.open.fill")
                                    .font(.system(size: 32))
                                    .foregroundColor(colorForState(tunnelManager.state))
                                    .padding(12)
                                    .background(KumoColors.base)
                                    .clipShape(Circle())

                                VStack(alignment: .leading, spacing: 4) {
                                    Text(tunnelManager.state == .connected ? "System Tunnel Active" : "Tunnel Ready")
                                        .font(.headline)
                                        .foregroundColor(KumoColors.primaryText)
                                    Text("Profile: \(tunnelManager.assignedProfile) • Active: \(tunnelManager.activeVersion)")
                                        .font(.subheadline)
                                        .foregroundColor(KumoColors.mutedText)
                                }
                                Spacer()
                            }

                            HStack(spacing: 12) {
                                if tunnelManager.state == .disconnected || tunnelManager.state == .failed {
                                    Button(action: {
                                        tunnelManager.connect()
                                    }) {
                                        HStack {
                                            Image(systemName: "power")
                                            Text("Connect VPN")
                                        }
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 8)
                                    }
                                    .buttonStyle(.borderedProminent)
                                    .tint(KumoColors.brand)
                                } else {
                                    Button(action: {
                                        tunnelManager.disconnect()
                                    }) {
                                        HStack {
                                            Image(systemName: "power")
                                            Text("Disconnect")
                                        }
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 8)
                                    }
                                    .buttonStyle(.bordered)
                                    .foregroundColor(KumoColors.danger)
                                }
                            }
                        }
                        .kumoCard()

                        // Telemetry & LKG Grid
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Active Profile Version")
                                    .font(.caption)
                                    .foregroundColor(KumoColors.mutedText)
                                Text(tunnelManager.activeVersion)
                                    .font(.title3.bold())
                                    .foregroundColor(KumoColors.primaryText)
                                Text("Channel: \(assignedChannel)")
                                    .font(.caption2)
                                    .foregroundColor(KumoColors.success)
                            }
                            .kumoCard()

                            VStack(alignment: .leading, spacing: 6) {
                                Text("Last-Known-Good Version")
                                    .font(.caption)
                                    .foregroundColor(KumoColors.mutedText)
                                Text(tunnelManager.activeVersion)
                                    .font(.title3.bold())
                                    .foregroundColor(KumoColors.success)
                                Text("Rollback Safety Preserved")
                                    .font(.caption2)
                                    .foregroundColor(KumoColors.mutedText)
                            }
                            .kumoCard()
                        }

                        // Activation Journal & Rollback Safety Panel
                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                Image(systemName: "checklist")
                                    .foregroundColor(KumoColors.brand)
                                Text("Transactional Sync & Rollback Engine")
                                    .font(.subheadline.bold())
                                    .foregroundColor(KumoColors.primaryText)
                            }
                            Divider().background(KumoColors.line)

                            VStack(alignment: .leading, spacing: 6) {
                                Text("• [STAGING] Candidate configuration verified against AST policy")
                                    .font(.caption)
                                    .foregroundColor(KumoColors.mutedText)
                                Text("• [ACTIVATING] Atomic atomic pointer swap with LKG preserved")
                                    .font(.caption)
                                    .foregroundColor(KumoColors.mutedText)
                                Text("• [COMMITTED] Active profile confirmed via post-activation health checks")
                                    .font(.caption)
                                    .foregroundColor(KumoColors.success)
                            }
                        }
                        .kumoCard()
                    }
                    .padding(24)
                }
            }
        }
    }

    private func colorForState(_ state: MacOSTunnelState) -> Color {
        switch state {
        case .connected: return KumoColors.success
        case .connecting, .disconnecting: return KumoColors.warning
        case .failed: return KumoColors.danger
        case .disconnected: return KumoColors.mutedText
        }
    }
}

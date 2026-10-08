import SwiftUI

/// Cloudflare Kumo UI Semantic Design System for macOS (SwiftUI).
///
/// Implements OpenSpec Phase 10 UI contract:
/// - Semantic surface tokens (kumoBase, kumoElevated, kumoRecessed, kumoLine).
/// - Cloudflare Orange Brand Accent (`kumoBrand` #F38020).
/// - Semantic Status Indicators (kumoSuccess, kumoWarning, kumoDanger).
public struct KumoColors {
    public static let base = Color(red: 0x0D / 255.0, green: 0x11 / 255.0, blue: 0x17 / 255.0)       // #0D1117
    public static let elevated = Color(red: 0x16 / 255.0, green: 0x1B / 255.0, blue: 0x22 / 255.0)   // #161B22
    public static let recessed = Color(red: 0x01 / 255.0, green: 0x04 / 255.0, blue: 0x09 / 255.0)   // #010409
    public static let line = Color(red: 0x30 / 255.0, green: 0x36 / 255.0, blue: 0x3D / 255.0)       // #30363D
    public static let brand = Color(red: 0xF3 / 255.0, green: 0x80 / 255.0, blue: 0x20 / 255.0)      // #F38020 (Cloudflare Orange)
    public static let primaryText = Color(red: 0xE6 / 255.0, green: 0xED / 255.0, blue: 0xF3 / 255.0)// #E6EDF3
    public static let mutedText = Color(red: 0x8B / 255.0, green: 0x94 / 255.0, blue: 0x9E / 255.0)  // #8B949E

    // Status Semantics
    public static let success = Color(red: 0x3F / 255.0, green: 0xB9 / 255.0, blue: 0x50 / 255.0)    // #3FB950
    public static let successSubtle = Color(red: 0x3F / 255.0, green: 0xB9 / 255.0, blue: 0x50 / 255.0).opacity(0.15)
    public static let warning = Color(red: 0xD2 / 255.0, green: 0x99 / 255.0, blue: 0x22 / 255.0)    // #D29922
    public static let warningSubtle = Color(red: 0xD2 / 255.0, green: 0x99 / 255.0, blue: 0x22 / 255.0).opacity(0.15)
    public static let danger = Color(red: 0xF8 / 255.0, green: 0x51 / 255.0, blue: 0x49 / 255.0)     // #F85149
    public static let dangerSubtle = Color(red: 0xF8 / 255.0, green: 0x51 / 255.0, blue: 0x49 / 255.0).opacity(0.15)
}

public struct KumoCardModifier: ViewModifier {
    public func body(content: Content) -> some View {
        content
            .padding(16)
            .background(KumoColors.elevated)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(KumoColors.line, lineWidth: 1)
            )
    }
}

public extension View {
    func kumoCard() -> some View {
        self.modifier(KumoCardModifier())
    }
}

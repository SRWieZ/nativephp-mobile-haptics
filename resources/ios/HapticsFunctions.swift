import Foundation
import UIKit

// MARK: - Haptics Function Namespace

/// Haptic feedback functions backed by UIFeedbackGenerator.
/// Namespace: "Haptics.*"
///
/// Haptics are fire-and-forget: each function dispatches the generator call to
/// the main thread and returns `["success": true]` synchronously. Raw vibrate
/// and pattern are approximated with repeated impacts (iOS exposes no raw
/// vibration API).
enum HapticsFunctions {

    // MARK: - Haptics.Impact

    class Impact: BridgeFunction {
        func execute(parameters: [String: Any]) throws -> [String: Any] {
            let style = parameters["style"] as? String ?? "medium"

            let feedbackStyle: UIImpactFeedbackGenerator.FeedbackStyle = {
                switch style {
                case "light": return .light
                case "heavy": return .heavy
                case "rigid": return .rigid
                case "soft": return .soft
                default: return .medium
                }
            }()

            // Optional repeat: `times` impacts `interval` ms apart, so a burst
            // stays a series of distinct taps instead of merging into one.
            let times = max(1, min(100, parameters["times"] as? Int ?? 1))
            let interval = Double(max(15, parameters["interval"] as? Int ?? 50)) / 1000.0

            DispatchQueue.main.async {
                let generator = UIImpactFeedbackGenerator(style: feedbackStyle)
                generator.prepare()
                generator.impactOccurred()

                for i in 1..<times {
                    DispatchQueue.main.asyncAfter(deadline: .now() + interval * Double(i)) {
                        generator.impactOccurred()
                    }
                }
            }

            return ["success": true]
        }
    }

    // MARK: - Haptics.Notification

    class Notification: BridgeFunction {
        func execute(parameters: [String: Any]) throws -> [String: Any] {
            let type = parameters["type"] as? String ?? "success"

            let feedbackType: UINotificationFeedbackGenerator.FeedbackType = {
                switch type {
                case "warning": return .warning
                case "error": return .error
                default: return .success
                }
            }()

            DispatchQueue.main.async {
                let generator = UINotificationFeedbackGenerator()
                generator.prepare()
                generator.notificationOccurred(feedbackType)
            }

            return ["success": true]
        }
    }

    // MARK: - Haptics.Selection

    class Selection: BridgeFunction {
        func execute(parameters: [String: Any]) throws -> [String: Any] {
            DispatchQueue.main.async {
                let generator = UISelectionFeedbackGenerator()
                generator.prepare()
                generator.selectionChanged()
            }

            return ["success": true]
        }
    }

    // MARK: - Haptics.Vibrate

    class Vibrate: BridgeFunction {
        func execute(parameters: [String: Any]) throws -> [String: Any] {
            let duration = (parameters["duration"] as? Int) ?? 200
            let clampedDuration = max(1, min(5000, duration))

            // iOS exposes no raw vibration API - approximate with repeated
            // heavy impacts proportional to the requested duration.
            DispatchQueue.main.async {
                let generator = UIImpactFeedbackGenerator(style: .heavy)
                generator.prepare()

                let repetitions = max(1, clampedDuration / 100)
                let interval = Double(clampedDuration) / Double(repetitions) / 1000.0

                for i in 0..<repetitions {
                    DispatchQueue.main.asyncAfter(deadline: .now() + interval * Double(i)) {
                        generator.impactOccurred()
                    }
                }
            }

            return ["success": true]
        }
    }

    // MARK: - Haptics.Pattern

    class Pattern: BridgeFunction {
        func execute(parameters: [String: Any]) throws -> [String: Any] {
            guard let pattern = parameters["pattern"] as? [Int], !pattern.isEmpty else {
                throw BridgeError.invalidParameters("pattern must contain at least one duration")
            }

            // Pattern: alternating vibrate/pause durations in ms. Approximate
            // the vibrate segments with repeated impact haptics.
            DispatchQueue.main.async {
                let generator = UIImpactFeedbackGenerator(style: .heavy)
                generator.prepare()

                var offset: Double = 0

                for (index, ms) in pattern.enumerated() {
                    let clampedMs = max(1, min(5000, ms))
                    let isVibrate = index % 2 == 0

                    if isVibrate {
                        let repetitions = max(1, clampedMs / 100)
                        let interval = Double(clampedMs) / Double(repetitions) / 1000.0

                        for i in 0..<repetitions {
                            let delay = offset + interval * Double(i)
                            DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
                                generator.impactOccurred()
                            }
                        }
                    }

                    offset += Double(clampedMs) / 1000.0
                }
            }

            return ["success": true]
        }
    }
}

import AVFoundation
import AudioToolbox
import ComposeApp

// Plays a looping ringtone for in-app (foreground) incoming calls.
// Uses AVAudioPlayer with .playback audio session so it rings even when
// the ringer/silent switch is OFF — matching Android's IncomingCallActivity behaviour.
// CallKit handles background/locked ringing automatically via its native UI.

@objc class RingtoneManager: NSObject, IOSRingtoneDelegate {

    @objc static let shared = RingtoneManager()

    private var player: AVAudioPlayer?
    private(set) var isRinging = false

    // MARK: - Public API

    func startRinging() {
        guard !isRinging else { return }
        isRinging = true

        // .playback category plays audio even when ringer switch is off.
        // This is critical — AudioServicesPlaySystemSound respects silent mode but AVAudioPlayer with
        // .playback does not.
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .default, options: [.mixWithOthers])
            try session.setActive(true)
        } catch {
            print("⚠️ [Ringtone] Audio session setup failed: \(error)")
        }

        // 1. Bundled ringtone (add ringtone.mp3 or ringtone.caf to Xcode project for best experience)
        if let url = Bundle.main.url(forResource: "ringtone", withExtension: "mp3")
                  ?? Bundle.main.url(forResource: "ringtone", withExtension: "caf"),
           play(url: url) {
            print("🔔 [Ringtone] Playing bundled ringtone")
            return
        }

        // 2. Known system sound files (short but looped → audible repeating alert)
        let systemPaths = [
            "/System/Library/Audio/UISounds/ReceivedMessage.caf",   // iMessage notification tone
            "/System/Library/Audio/UISounds/SIMToolkitCallDropped.caf",
            "/System/Library/Audio/UISounds/tweet_sent.caf",
            "/System/Library/Audio/UISounds/nano/ReceiveMessage.caf"
        ]
        for path in systemPaths {
            let url = URL(fileURLWithPath: path)
            if FileManager.default.fileExists(atPath: path), play(url: url) {
                print("🔔 [Ringtone] Playing system sound: \(path)")
                return
            }
        }

        // 3. Last-resort fallback: vibration loop (no sound file found)
        print("🔔 [Ringtone] No sound file found — vibration-only fallback")
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
        Timer.scheduledTimer(withTimeInterval: 1.5, repeats: true) { [weak self] t in
            guard let self = self, self.isRinging else { t.invalidate(); return }
            AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
        }
    }

    func stopRinging() {
        guard isRinging else { return }
        isRinging = false
        player?.stop()
        player = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        print("🔇 [Ringtone] Stopped")
    }

    // MARK: - IOSRingtoneDelegate (called by Kotlin stopCallSound() / cancelCallNotification())

    func stopRingtone() {
        stopRinging()
    }

    // MARK: - Private

    @discardableResult
    private func play(url: URL) -> Bool {
        guard let p = try? AVAudioPlayer(contentsOf: url) else { return false }
        p.numberOfLoops = -1   // loop forever
        p.volume = 1.0
        p.prepareToPlay()
        let started = p.play()
        if started { player = p }
        return started
    }
}
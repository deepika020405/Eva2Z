import AVFoundation
import AudioToolbox
import ComposeApp

// Plays a looping ringtone for in-app (foreground) incoming calls.
// Uses AVAudioPlayer / AVAudioEngine with .playback audio session so it rings even when
// the ringer/silent switch is OFF — matching Android's IncomingCallActivity behaviour.
// CallKit handles background/locked ringing automatically via its native UI.

@objc class RingtoneManager: NSObject, IOSRingtoneDelegate {

    @objc static let shared = RingtoneManager()

    private var player: AVAudioPlayer?
    private var audioEngine: AVAudioEngine?
    private var playerNode: AVAudioPlayerNode?
    private(set) var isRinging = false

    // MARK: - Public API

    func startRinging() {
        guard !isRinging else { return }
        isRinging = true

        // .playback category plays audio even when ringer switch is off.
        // .duckOthers lowers other audio so the ring is clearly heard.
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .default, options: [.duckOthers])
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

        // 2. Known system sound files present on modern iOS
        let systemPaths = [
            "/System/Library/Audio/UISounds/ReceivedMessage.caf",
            "/System/Library/Audio/UISounds/sms-received1.caf",
            "/System/Library/Audio/UISounds/sms-received5.caf",
            "/System/Library/Audio/UISounds/new-mail.caf",
            "/System/Library/Audio/UISounds/Tock.caf",
        ]
        for path in systemPaths {
            let url = URL(fileURLWithPath: path)
            if FileManager.default.fileExists(atPath: path), play(url: url) {
                print("🔔 [Ringtone] Playing system sound: \(path)")
                return
            }
        }

        // 3. Generate a dual-tone PSTN ring (440 Hz + 480 Hz) using AVAudioEngine.
        //    No external file needed; .playback session keeps it audible on silent mode.
        print("🔔 [Ringtone] No sound file found — generating ring tone")
        playGeneratedRingtone()
    }

    func stopRinging() {
        guard isRinging else { return }
        isRinging = false
        player?.stop()
        player = nil
        playerNode?.stop()
        playerNode = nil
        audioEngine?.stop()
        audioEngine = nil
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

    // Generates a 1-second dual-tone ring followed by 0.5 seconds of silence, looped.
    // 440 Hz + 480 Hz produces the classic North American PSTN ring sound.
    private func playGeneratedRingtone() {
        let sampleRate: Double = 44100
        let ringDuration: Double  = 1.0
        let totalDuration: Double = 1.5   // 1s ring + 0.5s silence

        let totalFrames = AVAudioFrameCount(sampleRate * totalDuration)
        let ringFrames  = AVAudioFrameCount(sampleRate * ringDuration)

        let format = AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 1)!
        guard let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: totalFrames),
              let data   = buffer.floatChannelData?[0] else {
            fallbackVibration()
            return
        }
        buffer.frameLength = totalFrames

        let freq1: Double = 440.0
        let freq2: Double = 480.0
        let fadeFrames    = Int(sampleRate * 0.05)   // 50 ms fade in/out

        for i in 0..<Int(totalFrames) {
            if i < Int(ringFrames) {
                let envelope: Double
                if i < fadeFrames {
                    envelope = Double(i) / Double(fadeFrames)
                } else if i > Int(ringFrames) - fadeFrames {
                    envelope = Double(Int(ringFrames) - i) / Double(fadeFrames)
                } else {
                    envelope = 1.0
                }
                let t = Double(i) / sampleRate
                data[i] = Float((sin(2 * .pi * freq1 * t) + sin(2 * .pi * freq2 * t)) * 0.4 * envelope)
            } else {
                data[i] = 0
            }
        }

        let engine = AVAudioEngine()
        let node   = AVAudioPlayerNode()
        engine.attach(node)
        engine.connect(node, to: engine.mainMixerNode, format: format)

        do {
            try engine.start()
            node.scheduleBuffer(buffer, at: nil, options: .loops, completionHandler: nil)
            node.play()
            audioEngine = engine
            playerNode  = node
            print("🔔 [Ringtone] Generated ring tone playing")
        } catch {
            print("⚠️ [Ringtone] AVAudioEngine failed: \(error)")
            fallbackVibration()
        }
    }

    private func fallbackVibration() {
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
        Timer.scheduledTimer(withTimeInterval: 1.5, repeats: true) { [weak self] t in
            guard let self = self, self.isRinging else { t.invalidate(); return }
            AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
        }
    }
}
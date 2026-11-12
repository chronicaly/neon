package me.xchro.neon.ui;

import java.io.BufferedInputStream;
import java.util.EnumMap;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.*;
import me.xchro.neon.Neon;
import me.xchro.neon.core.UiSoundMailbox;
import me.xchro.neon.core.UiSoundPolicy;
import me.xchro.neon.core.UiSoundPolicy.Cue;
import me.xchro.neon.core.UiSoundPolicy.Request;

/** Original soft console cues on Neon's independent, asynchronous audio line. */
public final class UiSounds {
    private static final ExecutorService AUDIO = Executors.newSingleThreadExecutor(r -> { var t = new Thread(r, "Neon UI audio"); t.setDaemon(true); return t; });
    private static final EnumMap<Cue, Clip> CLIPS = new EnumMap<>(Cue.class);
    private static final UiSoundMailbox MAILBOX = new UiSoundMailbox();
    private static final UiSoundPolicy POLICY = new UiSoundPolicy();
    private static final AtomicBoolean DRAINING = new AtomicBoolean();
    private static Clip actionVoice, hoverVoice;
    private static boolean prepared;
    private static volatile boolean closed;
    private UiSounds() {}
    public static void beginInput() { POLICY.begin(); }
    public static void endInput() { emit(POLICY.end(System.nanoTime(), Neon.config.theme.soundVolume)); }
    public static void input(Runnable action) { beginInput(); try { action.run(); } finally { endInput(); } }
    public static void cue(Cue cue) { emit(POLICY.request(cue, System.nanoTime(), Neon.config.theme.soundVolume)); }
    public static void toggled(boolean on) { cue(on ? Cue.ENABLE : Cue.DISABLE); }
    public static void moduleEnabled() { cue(Cue.ENABLE); }
    public static void chatPing(boolean mention, double volume) {
        if (Double.isFinite(volume) && volume > 0)
            emit(new Request(mention ? Cue.CHAT_MENTION : Cue.CHAT_MESSAGE, Math.min(1, volume), System.nanoTime()));
    }
    static void hover(String target) { emit(POLICY.hover(target, System.nanoTime(), Neon.config.theme.soundVolume)); }
    static void rememberHover(String target) { POLICY.remember(target); }
    private static void emit(Request request) { if (request != null && !closed) play(request); }
    private static void prepare() {
        if (prepared) return;
        prepared = true;
        for (Cue cue : Cue.values()) {
            Clip clip = null;
            try (var data = new BufferedInputStream(UiSounds.class.getResourceAsStream("/assets/neon/sounds/ui/" + cue.name().toLowerCase(Locale.ROOT) + ".wav"));
                 var audio = AudioSystem.getAudioInputStream(data)) {
                clip = AudioSystem.getClip(); clip.open(audio);
                // Never fall back to full-volume playback on a device without gain control.
                if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) { clip.close(); continue; }
                CLIPS.put(cue, clip);
            } catch (Exception failure) {
                if (clip != null) clip.close();
                Neon.LOG.warn("Neon UI audio unavailable for {}", cue, failure);
            }
        }
    }
    public static void warm() { if (!closed) AUDIO.execute(UiSounds::prepare); }
    // The verification harness intercepts this boundary before any audio is queued.
    private static void play(Request request) { MAILBOX.offer(request); drain(); }
    private static void drain() {
        if (!closed && DRAINING.compareAndSet(false, true)) AUDIO.execute(() -> {
            try {
                prepare();
                Request request;
                while (!closed && (request = MAILBOX.poll(System.nanoTime())) != null) {
                    Clip clip = CLIPS.get(request.cue());
                    if (clip == null) continue;
                    if (request.cue() == Cue.HOVER) { fadeOut(hoverVoice, null); hoverVoice = clip; }
                    else {
                        fadeOut(hoverVoice, actionVoice);
                        actionVoice = clip;
                    }
                    clip.stop(); clip.setFramePosition(0);
                    var gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    gain.setValue(Math.clamp((float) (20 * Math.log10(request.volume())), gain.getMinimum(), gain.getMaximum()));
                    clip.start();
                }
            } finally { DRAINING.set(false); if (!MAILBOX.empty()) drain(); }
        });
    }
    /** A bounded 4 ms release on the audio worker prevents hard cuts during rapid navigation. */
    private static void fadeOut(Clip first, Clip second) {
        FloatControl a = runningGain(first), b = runningGain(second);
        if (a == null && b == null) return;
        float startA = a == null ? 0 : a.getValue(), startB = b == null ? 0 : b.getValue();
        for (int step = 1; step <= 4; step++) {
            float attenuation = step == 4 ? 80 : step * 8;
            if (a != null) a.setValue(Math.max(a.getMinimum(), startA - attenuation));
            if (b != null) b.setValue(Math.max(b.getMinimum(), startB - attenuation));
            java.util.concurrent.locks.LockSupport.parkNanos(1_000_000L);
        }
        if (first != null) first.stop();
        if (second != null) second.stop();
    }
    private static FloatControl runningGain(Clip clip) {
        return clip != null && clip.isRunning() ? (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN) : null;
    }
    public static void close() {
        if (closed) return;
        closed = true; MAILBOX.clear();
        AUDIO.execute(() -> { CLIPS.values().forEach(Clip::close); CLIPS.clear(); }); AUDIO.shutdown();
    }
}

package me.xchro.neon.smoke;

import me.xchro.neon.ui.PixelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;

/** Verification-only observation; never changes playback or ships in Neon. */
public final class SoundAudit {
    public static final java.util.List<me.xchro.neon.core.UiSoundPolicy.Request> cues = new java.util.ArrayList<>();
    /** Legacy enable-only counter; the full audit is in cues. */
    public static int vanillaClicks, neonEnables;
    public static double lastVolume;
    public static void played(me.xchro.neon.core.UiSoundPolicy.Request request) {
        cues.add(request); lastVolume = request.volume();
        if (request.cue() == me.xchro.neon.core.UiSoundPolicy.Cue.ENABLE) neonEnables++;
    }
    public static void played(SoundInstance sound) {
        if (!(Minecraft.getInstance().gui.screen() instanceof PixelScreen)) return;
        String id = sound.getIdentifier().toString();
        if (id.equals("minecraft:ui.button.click")) vanillaClicks++;
        if (id.equals("neon:ui.enable")) neonEnables++;
    }
}

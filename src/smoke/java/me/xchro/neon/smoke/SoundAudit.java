package me.xchro.neon.smoke;

import me.xchro.neon.ui.PixelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;

/** Verification-only observation; never changes playback or ships in Neon. */
public final class SoundAudit {
    public static int vanillaClicks, neonClicks;
    public static double lastVolume;
    public static void played(SoundInstance sound) {
        if (!(Minecraft.getInstance().gui.screen() instanceof PixelScreen)) return;
        String id = sound.getIdentifier().toString();
        if (id.equals("minecraft:ui.button.click")) vanillaClicks++;
        if (id.startsWith("neon:ui.")) neonClicks++;
    }
}

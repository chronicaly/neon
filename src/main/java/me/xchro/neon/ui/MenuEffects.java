package me.xchro.neon.ui;

import me.xchro.neon.Neon;
import me.xchro.neon.config.NeonConfig.Effects;
import me.xchro.neon.core.AdaptiveEffects;
import net.minecraft.client.Minecraft;

public final class MenuEffects {
    private static final AdaptiveEffects QUALITY = new AdaptiveEffects();
    private static long monitorChecked;
    private static int refresh = 60;
    private static boolean active;
    public static void frame() {
        var mc = Minecraft.getInstance(); long now = System.nanoTime();
        active = mc.gui.screen() instanceof PixelScreen && !(mc.gui.screen() instanceof HudEditor);
        if (active && now - monitorChecked > 1_000_000_000L) {
            var monitor = mc.getWindow().findBestMonitor();
            refresh = monitor == null ? 60 : monitor.currentMode().getRefreshRate(); monitorChecked = now;
        }
        QUALITY.frame(now, AdaptiveEffects.budgetNanos(refresh, mc.options.framerateLimit().get()), active, Neon.config.theme.effects == Effects.FULL);
    }
    static boolean blur() { return !active || Neon.config.theme.effects == Effects.FULL || QUALITY.tier() == 0; }
    static boolean glow() { return !active || Neon.config.theme.effects == Effects.FULL || QUALITY.tier() < 2; }
}

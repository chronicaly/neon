package me.xchro.neon.input;

import me.xchro.neon.Neon;
import me.xchro.neon.core.*;
import me.xchro.neon.mixin.KeyAccessor;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.HitResult;

public final class AutoClicker {
    private final Activation activation = new Activation();
    private final ClickClock left = new ClickClock(), right = new ClickClock();
    private boolean active, mining;
    private Object level, player;
    public boolean active() { return active; }
    public void tick(Minecraft mc) {
        if (Neon.autoclicker == null) return;
        boolean safe = Neon.ready(mc) && Neon.config.clickerEnabled;
        if (level != mc.level || player != mc.player) { activation.reset(); safe = false; }
        level = mc.level; player = mc.player;
        boolean next = activation.update(safe, Neon.autoclicker.isDown(), Neon.config.toggleClicker);
        if (next != active && mc.player != null) mc.gui.hud.setOverlayMessage(Component.literal("Autoclicker: " + (next ? "on" : "off")), false);
        active = next;
        if (!active) {
            if (mining && mc.gameMode != null && !mc.options.keyAttack.isDown()) mc.gameMode.stopDestroyBlock();
            resetTiming();
        }
    }
    public void resetTiming() { left.reset(); right.reset(); mining = false; }
    public void apply(Minecraft mc) {
        if (!active || !Neon.ready(mc) || !Neon.config.clickerEnabled) { resetTiming(); return; }
        long now = System.nanoTime();
        boolean block = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;
        if (Neon.config.leftClick && !mc.player.isUsingItem()) {
            if (Neon.config.continuousMining && block) {
                inject(mc.options.keyAttack, true, !mining); mining = true; left.reset();
            } else {
                mining = false;
                boolean press = left.press(now, Neon.config.leftCps);
                inject(mc.options.keyAttack, press, press);
            }
        } else { mining = false; left.reset(); }
        if (Neon.config.rightClick) {
            boolean press = right.press(now, Neon.config.rightCps);
            inject(mc.options.keyUse, press, press);
        } else right.reset();
    }
    private static void inject(KeyMapping key, boolean down, boolean press) {
        if (down) key.setDown(true);
        KeyAccessor access = (KeyAccessor) key;
        if (press && access.neon$getClicks() == 0) access.neon$setClicks(1);
    }
}

package me.xchro.neon.input;

import me.xchro.neon.Neon;
import me.xchro.neon.core.*;
import me.xchro.neon.config.NeonConfig.LeftAction;
import me.xchro.neon.mixin.KeyAccessor;
import net.minecraft.client.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

public final class AutoClicker {
    private final ClickerActivation activation = new ClickerActivation();
    private final ClickClock left = new ClickClock(), right = new ClickClock();
    private boolean active, holdingAttack;
    private long tick;
    private Object level, player;
    public boolean active() { return active; }
    public void cancel(Minecraft mc) {
        if (holdingAttack && mc.gameMode != null && !mc.options.keyAttack.isDown()) mc.gameMode.stopDestroyBlock();
        active = false; activation.reset(); resetTiming();
    }
    private boolean allowed(Minecraft mc) {
        return Neon.config.clickerEnabled && mc.player != null && mc.level != null && mc.player.isAlive()
            && !mc.player.isSpectator() && !mc.player.isSleeping() && mc.getCameraEntity() == mc.player
            && mc.gameMode != null && !mc.isPaused() && mc.gui.overlay() == null;
    }
    public boolean inScreen(Minecraft mc) { return active && allowed(mc) && mc.gui.screen() != null; }
    // Deliberately independent of isPaused(): this is what prevents the pause itself.
    public boolean keepRunningInBackground(Minecraft mc) {
        return active && Neon.config.clickerEnabled && mc.level != null && mc.player != null;
    }
    private boolean activationDown(Minecraft mc) {
        if (mc.gui.screen() == null && mc.isWindowActive() || Neon.autoclicker.isUnbound()) return Neon.autoclicker.isDown();
        var key = ((KeyAccessor) Neon.autoclicker).neon$getKey();
        if (key.getType() == InputConstants.Type.MOUSE)
            return GLFW.glfwGetMouseButton(mc.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
        if (key.getType() == InputConstants.Type.KEYSYM)
            return InputConstants.isKeyDown(mc.getWindow(), key.getValue());
        return Neon.autoclicker.isDown();
    }
    public void tick(Minecraft mc) {
        tick++;
        if (Neon.autoclicker == null) return;
        if (level != mc.level || player != mc.player) resetTiming();
        level = mc.level; player = mc.player;
        boolean interactive = mc.player != null && mc.level != null && mc.gui.screen() == null
            && mc.gui.overlay() == null && mc.isWindowActive();
        // Menu typing never toggles automation. Existing Toggle sessions survive every context change.
        boolean next = activation.update(Neon.config.clickerEnabled && !Neon.autoclicker.isUnbound(), interactive, activationDown(mc), Neon.config.clickerMode());
        if (next != active && mc.player != null) mc.gui.hud.setOverlayMessage(Component.literal("Autoclicker: " + (next ? "on" : "off")), false);
        active = next;
        if (!active) {
            if (holdingAttack && mc.gameMode != null && !mc.options.keyAttack.isDown()) mc.gameMode.stopDestroyBlock();
            resetTiming();
        }
    }
    public void resetTiming() { left.reset(); right.reset(); holdingAttack = false; }
    public record Clicks(boolean attackDown, boolean attackPress, boolean use) {
        private static final Clicks NONE = new Clicks(false, false, false);
    }
    public Clicks clicks(Minecraft mc, boolean attackReady, boolean useReady) {
        if (!active || !allowed(mc)) { resetTiming(); return Clicks.NONE; }
        boolean block = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;
        boolean attackDown = false, attackPress = false, use = false;
        if (Neon.config.leftClick && !mc.player.isUsingItem()) {
            if (Neon.config.leftAction == LeftAction.HOLD) {
                attackDown = true; attackPress = attackReady && !holdingAttack; holdingAttack = true; left.reset();
            } else {
                holdingAttack = false;
                // Click is attack-only: neither a queued click nor held input may touch a block.
                // Keep the attack key released even on a click so vanilla cannot continue mining.
                boolean targetAllowed = !Neon.config.clickerEntitiesOnly || mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY;
                if (!block && targetAllowed) attackPress = left.press(tick, Neon.config.leftIntervalTicks,
                    attackReady && mc.player.getAttackStrengthScale(0) >= 1);
            }
        } else { holdingAttack = false; left.reset(); }
        if (Neon.config.rightClick) {
            use = right.press(tick, Neon.config.rightIntervalTicks, useReady);
        } else right.reset();
        return new Clicks(attackDown, attackPress, use);
    }
    public void apply(Minecraft mc, boolean attackReady, boolean useReady) {
        Clicks clicks = clicks(mc, attackReady, useReady);
        inject(mc.options.keyAttack, clicks.attackDown(), clicks.attackPress());
        inject(mc.options.keyUse, clicks.use(), clicks.use());
    }
    private static void inject(KeyMapping key, boolean down, boolean press) {
        if (down) key.setDown(true);
        KeyAccessor access = (KeyAccessor) key;
        if (press && access.neon$getClicks() == 0) access.neon$setClicks(1);
    }
}

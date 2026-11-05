package me.xchro.neon.input;

import me.xchro.neon.Neon;
import me.xchro.neon.core.*;
import me.xchro.neon.config.NeonConfig.LeftAction;
import me.xchro.neon.mixin.KeyAccessor;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.item.ItemStack;

public final class AutoClicker {
    private final Activation activation = new Activation();
    private final ClickClock left = new ClickClock(), right = new ClickClock();
    private boolean active, holdingAttack;
    private long tick;
    private Object level, player;
    private String lastStopReason = "";
    public boolean active() { return active; }
    public String lastStopReason() { return lastStopReason; }
    public void cancel(Minecraft mc) {
        if (holdingAttack && mc.gameMode != null && !mc.options.keyAttack.isDown()) mc.gameMode.stopDestroyBlock();
        active = false; activation.reset(); resetTiming();
    }
    private boolean protectTool(Minecraft mc) {
        if (!Neon.config.clickerProtectTools || mc.player == null) return false;
        ItemStack main = mc.player.getMainHandItem(), off = mc.player.getOffhandItem();
        ItemStack endangered = (Neon.config.leftClick || Neon.config.rightClick) && needsRepair(main) ? main
            : Neon.config.rightClick && needsRepair(off) ? off : ItemStack.EMPTY;
        if (endangered.isEmpty()) return false;
        lastStopReason = "Autoclicker stopped: " + endangered.getHoverName().getString() + " has "
            + (endangered.getMaxDamage() - endangered.getDamageValue()) + " durability";
        cancel(mc); mc.gui.hud.setOverlayMessage(Component.literal(lastStopReason), false); return true;
    }
    private boolean needsRepair(ItemStack item) {
        return item.isDamageableItem() && item.getMaxDamage() - item.getDamageValue() <= Neon.config.clickerMinDurability;
    }
    private boolean allowed(Minecraft mc) {
        return Neon.config.clickerEnabled && mc.player != null && mc.level != null && mc.player.isAlive()
            && !mc.player.isSpectator() && !mc.player.isSleeping() && mc.getCameraEntity() == mc.player
            && !mc.isPaused() && mc.gui.overlay() == null
            && (mc.gui.screen() == null || mc.gui.screen() instanceof ChatScreen && Neon.config.clickerInChat)
            && (mc.isWindowActive() || Neon.config.clickerInBackground);
    }
    public boolean inChat(Minecraft mc) { return active && allowed(mc) && mc.gui.screen() instanceof ChatScreen; }
    public boolean keepRunningInBackground(Minecraft mc) {
        return active && Neon.config.toggleClicker && Neon.config.clickerInBackground && allowed(mc);
    }
    public void tick(Minecraft mc) {
        tick++;
        if (Neon.autoclicker == null) return;
        boolean safe = allowed(mc);
        if (level != mc.level || player != mc.player) { activation.reset(); safe = false; }
        level = mc.level; player = mc.player;
        boolean interactive = mc.gui.screen() == null && mc.isWindowActive();
        // Only continue an already activated toggle while typing or unfocused. Never read
        // activation presses in chat, where they may simply be part of the user's message.
        boolean next;
        if (safe && !interactive && active && Neon.config.toggleClicker) next = true;
        else next = activation.update(safe && interactive, Neon.autoclicker.isDown(), Neon.config.toggleClicker);
        if (next && !active) lastStopReason = "";
        if (next != active && mc.player != null) mc.gui.hud.setOverlayMessage(Component.literal("Autoclicker: " + (next ? "on" : "off")), false);
        active = next;
        if (active && protectTool(mc)) return;
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
        if (protectTool(mc)) return Clicks.NONE;
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

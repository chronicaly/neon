package me.xchro.neon.hud;

import net.minecraft.client.Minecraft;

/** Distinguish actual movement from a held/latched input waiting for the game to allow it. */
final class MovementHud extends HudCard {
    MovementHud() { super(HudKind.MOVEMENT); }
    @Override protected void update(Minecraft mc,boolean preview) {
        if(mc.player==null) { lines=preview?new String[]{"Sprint  Ready","Sneak  Off"}:new String[0];return; }
        lines=new String[]{"Sprint  "+status(mc.player.isSprinting(),mc.options.keySprint.isDown()),
            "Sneak  "+status(mc.player.isCrouching(),mc.options.keyShift.isDown())};
    }
    private static String status(boolean active,boolean held) { return active?"On":held?"Ready":"Off"; }
}

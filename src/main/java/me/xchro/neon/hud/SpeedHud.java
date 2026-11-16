package me.xchro.neon.hud;

import java.util.Locale;
import net.minecraft.client.Minecraft;

final class SpeedHud extends HudCard {
    SpeedHud() { super(HudKind.SPEED); }
    @Override protected void update(Minecraft mc,boolean preview) {
        double speed=mc.player==null||mc.level==null?0:HudNumbers.horizontalSpeed(
            mc.player.getX()-mc.player.xo,mc.player.getZ()-mc.player.zo,mc.level.tickRateManager().tickrate());
        lines=new String[]{String.format(Locale.ROOT,"SPEED  %.2f b/s",speed)};
    }
    @Override protected int contentWidth() { return Math.max(text.width("SPEED  00.00 b/s"),super.contentWidth()); }
}

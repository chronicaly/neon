package me.xchro.neon.hud;

import me.xchro.neon.Neon;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.LightLayer;

/** Raw block/skylight already loaded by the client; this does not predict mob spawning. */
final class LightHud extends HudCard {
    LightHud() { super(HudKind.LIGHT); }
    @Override protected void update(Minecraft mc,boolean preview) {
        if(mc.player==null||mc.level==null||!mc.level.hasChunkAt(mc.player.blockPosition())) { lines=new String[]{"LIGHT  —"};return; }
        var pos=mc.player.blockPosition();
        String value="LIGHT  Block "+mc.level.getBrightness(LightLayer.BLOCK,pos);
        lines=Neon.config.lightSky ? new String[]{value,"Sky "+mc.level.getBrightness(LightLayer.SKY,pos)} : new String[]{value};
    }
}

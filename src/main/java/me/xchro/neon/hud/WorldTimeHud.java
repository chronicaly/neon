package me.xchro.neon.hud;

import me.xchro.neon.Neon;
import net.minecraft.client.Minecraft;

/** Always reports the actual synchronized clock, independently of visual time changing. */
final class WorldTimeHud extends HudCard {
    WorldTimeHud() { super(HudKind.WORLD_TIME); }
    @Override protected long revision(Minecraft mc) { return mc.level==null?0:Math.floorDiv(mc.level.getOverworldClockTime(),10); }
    @Override protected void update(Minecraft mc,boolean preview) {
        if(mc.level==null&&!preview) { lines=new String[0];return; }
        long ticks=mc.level==null?6000:mc.level.getOverworldClockTime();
        String time="WORLD  "+HudNumbers.worldTime(ticks);
        lines=Neon.config.worldTimeDay?new String[]{time,"Day  "+Math.floorDiv(ticks,24000)}:new String[]{time};
    }
}

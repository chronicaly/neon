package me.xchro.neon.hud;

import net.minecraft.client.Minecraft;

/** This JVM's used heap and configured heap limit, sampled once a second. */
final class MemoryHud extends HudCard {
    MemoryHud() { super(HudKind.MEMORY); }
    @Override protected long revision(Minecraft mc) { return System.nanoTime()/1_000_000_000L; }
    @Override protected void update(Minecraft mc,boolean preview) {
        var runtime=Runtime.getRuntime();long used=runtime.totalMemory()-runtime.freeMemory(),limit=runtime.maxMemory();
        lines=new String[]{"MEMORY  "+Math.clamp(Math.round(used*100.0/Math.max(1,limit)),0,100)+"%",(used>>20)+" / "+(limit>>20)+" MiB"};
    }
}

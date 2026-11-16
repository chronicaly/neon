package me.xchro.neon.hud;

import me.xchro.neon.core.SessionClock;
import net.minecraft.client.Minecraft;

final class SessionHud extends HudCard {
    SessionHud() { super(HudKind.SESSION); }
    @Override protected long revision(Minecraft mc) { return System.nanoTime()/1_000_000_000L; }
    @Override protected void update(Minecraft mc,boolean preview) {
        lines=new String[]{"SESSION  "+SessionClock.format(HudWidgets.SESSION.seconds(System.nanoTime()))};
    }
}

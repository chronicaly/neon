package me.xchro.neon.hud;

import net.minecraft.client.Minecraft;

/** Uses the latency already supplied by the server player list; sends no extra requests. */
final class PingHud extends HudCard {
    PingHud() { super(HudKind.PING); }
    @Override protected void update(Minecraft mc,boolean preview) {
        if(mc.getSingleplayerServer()!=null) { lines=new String[]{"PING  Local"}; return; }
        var info=mc.player==null||mc.getConnection()==null?null:mc.getConnection().getPlayerInfo(mc.player.getUUID());
        lines=new String[]{"PING  "+(info==null?"—":Math.max(0,info.getLatency())+" ms")};
    }
    @Override protected int contentWidth() { return Math.max(text.width("PING  999 ms"),super.contentWidth()); }
}

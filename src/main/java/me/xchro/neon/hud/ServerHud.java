package me.xchro.neon.hud;

import java.util.ArrayList;
import me.xchro.neon.Neon;
import net.minecraft.client.Minecraft;

final class ServerHud extends HudCard {
    ServerHud() { super(HudKind.SERVER); }
    @Override protected void update(Minecraft mc,boolean preview) {
        var rows=new ArrayList<String>(3);var server=mc.getCurrentServer();
        rows.add("SERVER");
        rows.add(mc.getSingleplayerServer()!=null?"Singleplayer":server==null?"Not connected":server.name);
        if(Neon.config.serverAddress&&server!=null)rows.add(server.ip);
        if(Neon.config.serverPlayers&&mc.getConnection()!=null) {
            int count=mc.getConnection().getOnlinePlayers().size();rows.add(count+(count==1?" player":" players"));
        }
        lines=rows.toArray(String[]::new);
    }
    @Override protected int contentWidth() { return Math.min(220,super.contentWidth()); }
    @Override protected void renderContent(net.minecraft.client.gui.GuiGraphicsExtractor g,boolean preview) {
        var t=Neon.config.theme;
        for(int i=0;i<lines.length;i++)text.draw(g,text.trim(lines[i],220),5,3+i*(text.height()+3),
            t.color(i==0?me.xchro.neon.config.ThemeColor.ACCENT:me.xchro.neon.config.ThemeColor.TEXT),Neon.config.hud(kind).shadow);
    }
}

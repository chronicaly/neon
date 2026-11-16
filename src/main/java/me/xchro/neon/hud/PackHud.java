package me.xchro.neon.hud;

import java.util.*;
import me.xchro.neon.Neon;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackSource;

final class PackHud extends HudCard {
    PackHud() { super(HudKind.PACKS); }
    @Override protected long revision(Minecraft mc) { return System.nanoTime()/1_000_000_000L; }
    @Override protected void update(Minecraft mc,boolean preview) {
        var selected=new ArrayList<>(mc.getResourcePackRepository().getSelectedPacks());
        var names=new ArrayList<String>();
        for(var pack:selected.reversed()) {
            var source=pack.getPackSource();
            if(source==PackSource.DEFAULT||source==PackSource.SERVER||source==PackSource.WORLD) names.add(pack.getTitle().getString());
        }
        var rows=new ArrayList<String>();rows.add("RESOURCE PACKS");
        if(names.isEmpty())rows.add("Default / built-in");
        else {
            int count=Math.min(names.size(),Neon.config.packMaxRows);
            for(int i=0;i<count;i++)rows.add(names.get(i));
            if(names.size()>count)rows.add("+"+(names.size()-count)+" packs");
        }
        lines=rows.toArray(String[]::new);
    }
    @Override protected int contentWidth() { return Math.min(145,super.contentWidth()); }
    @Override protected void renderContent(net.minecraft.client.gui.GuiGraphicsExtractor g,boolean preview) {
        var theme=Neon.config.theme;
        for(int i=0;i<lines.length;i++) text.draw(g,text.trim(lines[i],145),5,3+i*(text.height()+3),
            theme.color(i==0?me.xchro.neon.config.ThemeColor.ACCENT:me.xchro.neon.config.ThemeColor.TEXT),Neon.config.hud(kind).shadow);
    }
}

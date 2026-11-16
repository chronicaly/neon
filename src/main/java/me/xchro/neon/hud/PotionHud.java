package me.xchro.neon.hud;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.ThemeColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.effect.*;

final class PotionHud extends HudCard {
    private String[] timers=new String[0];
    private int[] colors=new int[0];
    PotionHud() { super(HudKind.POTIONS); }
    @Override protected void update(Minecraft mc,boolean preview) {
        var effects=mc.player==null ? new ArrayList<MobEffectInstance>() : new ArrayList<>(mc.player.getActiveEffects());
        effects.sort(Comparator.comparing(effect->effect.getEffect().value().getDescriptionId()));
        if(effects.isEmpty()) {
            lines=preview?new String[]{"Speed 2","Fire Resistance"}:new String[0];
            timers=preview?new String[]{"1:30","8:00"}:new String[0];colors=preview?new int[]{0xff7cafc6,0xffe49a3a}:new int[0];return;
        }
        int count=Math.min(effects.size(),Neon.config.potionMaxRows),extra=effects.size()>count?1:0;
        lines=new String[count+extra];timers=new String[lines.length];colors=new int[lines.length];
        for(int i=0;i<count;i++) {
            var effect=effects.get(i);var type=effect.getEffect().value();
            lines[i]=type.getDisplayName().getString()+(effect.getAmplifier()>0?" "+(effect.getAmplifier()+1):"");
            timers[i]=MobEffectUtil.formatDuration(effect,1,mc.level.tickRateManager().tickrate()).getString();
            colors[i]=type.getColor()|0xff000000;
        }
        if(extra>0) { lines[count]="+"+(effects.size()-count)+" effects";timers[count]="";colors[count]=Neon.config.theme.color(ThemeColor.MUTED); }
    }
    @Override protected int contentWidth() { return 148; }
    @Override protected int contentHeight() { return Math.max(1,lines.length)*14; }
    @Override protected void renderContent(GuiGraphicsExtractor g,boolean preview) {
        var theme=Neon.config.theme;boolean shadow=Neon.config.hud(kind).shadow;
        for(int i=0;i<lines.length;i++) {
            int y=4+i*14;
            g.fill(5,y,7,y+9,colors[i]);
            int timeWidth=text.width(timers[i]);
            text.draw(g,text.trim(lines[i],138-timeWidth-8),11,y,theme.color(ThemeColor.TEXT),shadow);
            text.draw(g,timers[i],153-timeWidth,y,theme.color(ThemeColor.ACCENT),shadow);
        }
    }
}

package me.xchro.neon.hud;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.ThemeColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.*;

final class HeldItemHud extends HudCard {
    private final List<ItemStack> items=new ArrayList<>(2);
    private String[] details=new String[0];
    private static final class Preview { static final ItemStack PICKAXE=new ItemStack(Items.DIAMOND_PICKAXE); }
    HeldItemHud() { super(HudKind.HELD_ITEM); }
    @Override protected void update(Minecraft mc,boolean preview) {
        items.clear();
        if(mc.player!=null) {
            if(!mc.player.getMainHandItem().isEmpty())items.add(mc.player.getMainHandItem());
            if(Neon.config.heldItemOffhand&&!mc.player.getOffhandItem().isEmpty())items.add(mc.player.getOffhandItem());
        }
        if(items.isEmpty()&&preview)items.add(Preview.PICKAXE);
        lines=new String[items.size()];details=new String[items.size()];
        for(int i=0;i<items.size();i++) {
            var stack=items.get(i);lines[i]=stack.getHoverName().getString();
            if(stack.isDamageableItem()) {
                int remaining=Math.max(0,stack.getMaxDamage()-stack.getDamageValue());
                details[i]=Neon.config.heldItemPercent?(int)Math.ceil(remaining*100.0/stack.getMaxDamage())+"% durability":remaining+" / "+stack.getMaxDamage();
            } else details[i]=stack.getCount()+" carried in hand";
        }
    }
    @Override protected int contentWidth() {
        int width=24;for(int i=0;i<lines.length;i++)width=Math.max(width,21+Math.max(text.width(lines[i]),text.width(details[i])));
        return Math.min(220,width);
    }
    private int rowHeight(){return Math.max(16,text.height()*2+1);}
    @Override protected int contentHeight() { return Math.max(1,items.size())*rowHeight()+Math.max(0,items.size()-1)*2; }
    @Override protected void renderContent(GuiGraphicsExtractor g,boolean preview) {
        var mc=Minecraft.getInstance();var theme=Neon.config.theme;boolean shadow=Neon.config.hud(kind).shadow;
        for(int i=0;i<items.size();i++) {
            var stack=items.get(i);int y=3+i*(rowHeight()+2),itemY=y+(rowHeight()-16)/2,width=contentWidth()-21;
            g.item(stack,5,itemY);g.itemDecorations(mc.font,stack,5,itemY);
            text.draw(g,text.trim(lines[i],width),26,y,theme.color(ThemeColor.TEXT),shadow);
            int color=NeonHud.lowDurability(stack)?0xffffaa00:theme.color(ThemeColor.ACCENT);
            text.draw(g,text.trim(details[i],width),26,y+text.height()+1,color,shadow);
        }
    }
}

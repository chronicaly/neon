package me.xchro.neon.hud;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.ThemeColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.*;

final class HeldItemHud extends HudCard {
    private final List<ItemStack> items=new ArrayList<>(2);
    private static final class Preview { static final ItemStack PICKAXE=new ItemStack(Items.DIAMOND_PICKAXE); }
    HeldItemHud() { super(HudKind.HELD_ITEM); }
    @Override protected void update(Minecraft mc,boolean preview) {
        items.clear();
        if(mc.player!=null) {
            if(!mc.player.getMainHandItem().isEmpty())items.add(mc.player.getMainHandItem());
            if(Neon.config.heldItemOffhand&&!mc.player.getOffhandItem().isEmpty())items.add(mc.player.getOffhandItem());
        }
        if(items.isEmpty()&&preview)items.add(Preview.PICKAXE);
        lines=new String[items.size()];
        for(int i=0;i<items.size();i++) lines[i]=items.get(i).getHoverName().getString();
    }
    @Override protected int contentWidth() { return 144; }
    @Override protected int contentHeight() { return Math.max(1,items.size())*25; }
    @Override protected void renderContent(GuiGraphicsExtractor g,boolean preview) {
        var mc=Minecraft.getInstance();var theme=Neon.config.theme;boolean shadow=Neon.config.hud(kind).shadow;
        for(int i=0;i<items.size();i++) {
            var stack=items.get(i);int y=4+i*25;
            g.item(stack,5,y+2);g.itemDecorations(mc.font,stack,5,y+2);
            text.draw(g,text.trim(lines[i],122),26,y,theme.color(ThemeColor.TEXT),shadow);
            String details;
            if(stack.isDamageableItem()) {
                int remaining=Math.max(0,stack.getMaxDamage()-stack.getDamageValue());
                details=Neon.config.heldItemPercent ? (int)Math.ceil(remaining*100.0/stack.getMaxDamage())+"% durability" : remaining+" / "+stack.getMaxDamage();
            } else details=stack.getCount()+" carried in hand";
            int color=NeonHud.lowDurability(stack)?0xffffaa00:theme.color(ThemeColor.ACCENT);
            text.draw(g,text.trim(details,122),26,y+12,color,shadow);
        }
    }
}

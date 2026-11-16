package me.xchro.neon.hud;

import me.xchro.neon.Neon;
import me.xchro.neon.config.ThemeColor;
import me.xchro.neon.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.*;

final class InventoryHud extends HudCard {
    InventoryHud() { super(HudKind.INVENTORY); }
    @Override protected void update(Minecraft mc,boolean preview) { lines=new String[]{"Inventory"}; }
    @Override protected int contentWidth() { return 160; }
    @Override protected int contentHeight() { return text.height()+(Neon.config.inventoryHotbar?4:3)*18; }
    @Override protected void renderContent(GuiGraphicsExtractor g,boolean preview) {
        var mc=Minecraft.getInstance();var theme=Neon.config.theme;
        text.draw(g,"Inventory",5,3,theme.color(ThemeColor.ACCENT),Neon.config.hud(kind).shadow);
        int rows=Neon.config.inventoryHotbar?4:3;
        for(int row=0;row<rows;row++) for(int column=0;column<9;column++) {
            int slot=row==3?column:9+row*9+column,x=5+column*18,y=5+text.height()+row*18;
            if(Neon.config.inventorySlots&&HudChrome.backgrounds()) {
                int color=theme.color(ThemeColor.MUTED);
                g.fill(x,y,x+16,y+16,Ui.alpha(color,(int)((color>>>24)*.10)));
            }
            ItemStack stack=mc.player==null?ItemStack.EMPTY:mc.player.getInventory().getItem(slot);
            if(!stack.isEmpty()) { g.item(stack,x,y);g.itemDecorations(mc.font,stack,x,y); }
        }
    }
}

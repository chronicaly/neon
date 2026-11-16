package me.xchro.neon.hud;

import me.xchro.neon.Neon;
import me.xchro.neon.config.NeonConfig.CounterItem;
import me.xchro.neon.config.ThemeColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;

final class ItemCounterHud extends HudCard {
    private ItemStack icon=ItemStack.EMPTY;
    ItemCounterHud() { super(HudKind.ITEM_COUNTER); }
    @Override protected void update(Minecraft mc,boolean preview) {
        var choice=Neon.config.counterItem;
        Item item=switch(choice) {
            case HELD -> mc.player==null?Items.AIR:mc.player.getMainHandItem().getItem();
            case PEARLS -> Items.ENDER_PEARL; case ARROWS -> Items.ARROW; case ROCKETS -> Items.FIREWORK_ROCKET;
            case GOLDEN_APPLES -> Items.GOLDEN_APPLE; case ENCHANTED_APPLES -> Items.ENCHANTED_GOLDEN_APPLE;
            case EXPERIENCE -> Items.EXPERIENCE_BOTTLE;case OBSIDIAN -> Items.OBSIDIAN;case DIAMONDS -> Items.DIAMOND;
        };
        if(item==Items.AIR) { if(preview)item=Items.ENDER_PEARL;else { lines=new String[0];return; } }
        if(!icon.is(item))icon=new ItemStack(item);
        int total=0;
        if(mc.player!=null) {
            for(var stack:mc.player.getInventory().getNonEquipmentItems())if(matches(stack,item,choice))total+=stack.getCount();
            var offhand=mc.player.getOffhandItem();if(Neon.config.counterOffhand&&matches(offhand,item,choice))total+=offhand.getCount();
        }
        String name=choice==CounterItem.HELD?icon.getHoverName().getString():choice.label;
        lines=new String[]{name+"  "+total};
    }
    private static boolean matches(ItemStack stack,Item item,CounterItem choice) { return choice==CounterItem.ARROWS?stack.is(ItemTags.ARROWS):stack.is(item); }
    @Override protected int contentWidth() { return super.contentWidth()+21; }
    @Override protected int contentHeight() { return 18; }
    @Override protected void renderContent(GuiGraphicsExtractor g,boolean preview) {
        g.item(icon,5,4);text.draw(g,lines[0],26,7,Neon.config.theme.color(ThemeColor.ACCENT),Neon.config.hud(kind).shadow);
    }
}

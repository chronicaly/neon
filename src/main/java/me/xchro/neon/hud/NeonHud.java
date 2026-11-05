package me.xchro.neon.hud;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.NeonConfig.Widget;
import me.xchro.neon.ui.HudEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;

public final class NeonHud {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Item[] PREVIEW = {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS};
    public static List<ItemStack> armor(boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack stack = mc.player == null ? ItemStack.EMPTY : mc.player.getItemBySlot(SLOTS[i]);
            if (!stack.isEmpty()) result.add(stack);
            else if (preview) result.add(new ItemStack(PREVIEW[i]));
        }
        return result;
    }
    public static int totems() {
        var player = Minecraft.getInstance().player;
        if (player == null) return 0;
        int count = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) if (stack.is(Items.TOTEM_OF_UNDYING)) count += stack.getCount();
        if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) count += player.getOffhandItem().getCount();
        return count;
    }
    public static HudBounds bounds(boolean armor, int width, int height, boolean preview) {
        return HudBounds.of(armor ? Neon.config.armor : Neon.config.totems,
            armor ? Math.max(16, armor(preview).size() * 18 - 2) : 16, width, height);
    }
    public static void draw(GuiGraphicsExtractor graphics, boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        if (!preview && (mc.player == null || mc.gui.hud.isHidden() || mc.gui.screen() instanceof HudEditor)) return;
        List<ItemStack> armor = armor(preview);
        if (Neon.config.armor.visible && !armor.isEmpty()) drawItems(graphics, Neon.config.armor, armor, null);
        int count = totems();
        if (Neon.config.totems.visible && (preview || count > 0))
            drawItems(graphics, Neon.config.totems, List.of(new ItemStack(Items.TOTEM_OF_UNDYING)), Integer.toString(preview ? Math.max(1, count) : count));
    }
    private static void drawItems(GuiGraphicsExtractor g, Widget widget, List<ItemStack> items, String count) {
        var font = Minecraft.getInstance().font;
        HudBounds bounds = HudBounds.of(widget, items.size() * 18 - 2, g.guiWidth(), g.guiHeight());
        g.pose().pushMatrix();
        g.pose().translate(bounds.x(), bounds.y()); g.pose().scale((float) widget.scale);
        for (int i = 0; i < items.size(); i++) {
            g.item(items.get(i), i * 18, 0);
            g.itemDecorations(font, items.get(i), i * 18, 0, count);
        }
        g.pose().popMatrix();
    }
}

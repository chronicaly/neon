package me.xchro.neon.hud;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.ui.HudEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;

public final class NeonHud {
    private static final Identifier HOTBAR = Identifier.withDefaultNamespace("hud/hotbar");
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
        if (Neon.config.armorReverse) Collections.reverse(result);
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
    /** Overrides decorations on the actual offhand slot, never creates another item icon. */
    public static String offhandCount(ItemStack stack) {
        var player = Minecraft.getInstance().player;
        return Neon.config.totems.visible && player != null && stack == player.getOffhandItem()
            && stack.is(Items.TOTEM_OF_UNDYING) ? Integer.toString(totems()) : null;
    }
    public static int contentWidth(int count) { return Neon.config.armorVertical ? slotSize() : length(count); }
    public static int contentHeight(int count) { return Neon.config.armorVertical ? length(count) : slotSize(); }
    private static int slotSize() { return Neon.config.armorSlots ? 22 : 16; }
    private static int stride() { return (Neon.config.armorSlots ? 20 : 16) + Neon.config.armorGap; }
    private static int length(int count) { return slotSize() + Math.max(0, count - 1) * stride(); }
    public static HudBounds bounds(int width, int height, boolean preview) {
        int count = armor(preview).size();
        var b = HudBounds.of(Neon.config.armor, contentWidth(count), contentHeight(count), width, height);
        int right = width - b.width(), bottom = height - b.height();
        return switch (Neon.config.armorAnchor) {
            case CUSTOM -> b;
            case TOP_LEFT -> b.at(0, 0, width, height);
            case TOP_CENTER -> b.at(right / 2, 0, width, height);
            case TOP_RIGHT -> b.at(right, 0, width, height);
            case CENTER_LEFT -> b.at(0, bottom / 2, width, height);
            case CENTER_RIGHT -> b.at(right, bottom / 2, width, height);
            case BOTTOM_LEFT -> b.at(0, bottom, width, height);
            case BOTTOM_RIGHT -> b.at(right, bottom, width, height);
            case HOTBAR_LEFT -> b.at(width / 2 - 91 - hotbarSpace(true) - b.width(), bottom, width, height);
            case HOTBAR_RIGHT -> b.at(width / 2 + 91 + hotbarSpace(false), bottom, width, height);
        };
    }
    private static int hotbarSpace(boolean left) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return 2;
        boolean offhandLeft = mc.player.getMainArm() == HumanoidArm.RIGHT;
        if (left == offhandLeft && !mc.player.getOffhandItem().isEmpty()) return 31;
        if (left != offhandLeft && mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) return 23;
        return 2;
    }
    public static boolean lowDurability(ItemStack stack) {
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        return stack.isDamageableItem() && (remaining <= 5 || remaining * 100L <= stack.getMaxDamage() * (long) Neon.config.armorWarningPercent);
    }
    public static void draw(GuiGraphicsExtractor graphics, boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        if (!preview && (mc.player == null || mc.gui.hud.isHidden() || mc.gui.screen() instanceof HudEditor)) return;
        List<ItemStack> armor = armor(preview);
        if ((preview || Neon.config.armor.visible) && !armor.isEmpty()) drawItems(graphics, armor);
    }
    private static void drawItems(GuiGraphicsExtractor g, List<ItemStack> items) {
        var font = Minecraft.getInstance().font;
        HudBounds bounds = bounds(g.guiWidth(), g.guiHeight(), Minecraft.getInstance().gui.screen() instanceof HudEditor);
        float scale = (float) HudBounds.scale(Neon.config.armor, contentWidth(items.size()), contentHeight(items.size()), g.guiWidth(), g.guiHeight());
        g.pose().pushMatrix();
        g.pose().translate(bounds.x(), bounds.y()); g.pose().scale(scale);
        for (int i = 0; i < items.size(); i++) {
            int x = Neon.config.armorVertical ? 0 : i * stride(), y = Neon.config.armorVertical ? i * stride() : 0;
            if (Neon.config.armorSlots) {
                // Slice the native hotbar ends into one slot; resource packs supply its texture.
                g.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, 182, 22, 0, 0, x, y, 21, 22);
                g.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, 182, 22, 181, 0, x + 21, y, 1, 22);
                x += 3; y += 3;
            }
            ItemStack stack = items.get(i);
            g.item(stack, x, y);
            g.itemDecorations(font, stack, x, y);
            if (Neon.config.armorWarnings && lowDurability(stack)) g.text(font, "!", x + 12, y, 0xffffaa00);
        }
        g.pose().popMatrix();
    }
}

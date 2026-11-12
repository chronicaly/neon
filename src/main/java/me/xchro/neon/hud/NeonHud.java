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
    private static final class Stacks {
        static final ItemStack[] PREVIEW_STACKS = Arrays.stream(PREVIEW).map(ItemStack::new).toArray(ItemStack[]::new);
        static final ItemStack TOTEM_STACK = new ItemStack(Items.TOTEM_OF_UNDYING);
    }
    private static final ItemStack[] damageSources = new ItemStack[4], damaged = new ItemStack[4];
    private static final List<ItemStack> normalFrame = new ArrayList<>(4), previewFrame = new ArrayList<>(4);
    private static boolean extracting, normalReady, previewReady;
    private static int frameTotems = -1;
    public static void beginFrame() { extracting = true; normalReady = previewReady = false; frameTotems = -1; me.xchro.neon.ui.HudText.beginFrame(); }
    public static void endFrame() { extracting = false; me.xchro.neon.ui.HudText.flush(); }
    public static List<ItemStack> armor(boolean preview) {
        if (extracting && (preview ? previewReady : normalReady)) return preview ? previewFrame : normalFrame;
        Minecraft mc = Minecraft.getInstance();
        List<ItemStack> result = extracting ? preview ? previewFrame : normalFrame : new ArrayList<>(4);
        result.clear();
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack stack = mc.player == null ? ItemStack.EMPTY : mc.player.getItemBySlot(SLOTS[i]);
            if (stack.isEmpty() && preview) stack = Stacks.PREVIEW_STACKS[i];
            if (preview && mc.gui.screen() instanceof HudEditor editor && editor.damagePreview() && stack.isDamageableItem()) {
                if (damageSources[i] == null || !ItemStack.matches(stack, damageSources[i])) {
                    damageSources[i] = stack.copy(); damaged[i] = stack.copy(); damaged[i].setDamageValue(stack.getMaxDamage() - 1);
                }
                stack = damaged[i];
            }
            if (!stack.isEmpty()) result.add(stack);
        }
        if (Neon.config.armorReverse) Collections.reverse(result);
        if (extracting) { if (preview) previewReady = true; else normalReady = true; }
        return result;
    }
    public static int totems() {
        if (extracting && frameTotems >= 0) return frameTotems;
        var player = Minecraft.getInstance().player;
        if (player == null) return 0;
        int count = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) if (stack.is(Items.TOTEM_OF_UNDYING)) count += stack.getCount();
        if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) count += player.getOffhandItem().getCount();
        if (extracting) frameTotems = count;
        return count;
    }
    /** Overrides decorations on the actual offhand slot, never creates another item icon. */
    public static String offhandCount(ItemStack stack) {
        var player = Minecraft.getInstance().player;
        return Neon.config.totems.visible && !Neon.config.totemFloating && player != null && stack == player.getOffhandItem()
            && stack.is(Items.TOTEM_OF_UNDYING) ? Integer.toString(totems()) : null;
    }
    public static int contentWidth(int count) { return Neon.config.armorVertical ? slotSize() : length(count); }
    public static int contentHeight(int count) { return Neon.config.armorVertical ? length(count) : slotSize(); }
    private static int slotSize() { return Neon.config.armorSlots ? 22 : 16; }
    private static int stride() { return (Neon.config.armorSlots ? 20 : 16) + Neon.config.armorGap; }
    private static int length(int count) { return slotSize() + Math.max(0, count - 1) * stride(); }
    public static HudBounds bounds(int width, int height, boolean preview) {
        int scale=gameScale();return toGui(pixelBounds(width*scale,height*scale,preview),scale);
    }
    public static int gameScale(){return Math.max(1,Minecraft.getInstance().getWindow().getGuiScale());}
    public static double density(){return 2*Neon.config.hudScale;}
    private static HudBounds toGui(HudBounds b,int scale){return new HudBounds((int)Math.round(b.x()/(double)scale),(int)Math.round(b.y()/(double)scale),(int)Math.ceil(b.width()/(double)scale),(int)Math.ceil(b.height()/(double)scale));}
    public static HudBounds pixelBounds(int width,int height,boolean preview){
        int count = armor(preview).size();
        var b = HudBounds.of(Neon.config.armor, contentWidth(count), contentHeight(count), width, height,density());
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
            case HOTBAR_LEFT -> b.at(hotbarCenter(width) - (91 + hotbarSpace(true))*gameScale() - b.width(), bottom, width, height);
            case HOTBAR_RIGHT -> b.at(hotbarCenter(width) + (91 + hotbarSpace(false))*gameScale(), bottom, width, height);
        };
    }
    private static int hotbarCenter(int width) { int scale = gameScale(); return ((width + scale - 1) / scale) / 2 * scale; }
    private static int hotbarSpace(boolean left) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return 2;
        boolean offhandLeft = mc.player.getMainArm() == HumanoidArm.RIGHT;
        if (left == offhandLeft && !mc.player.getOffhandItem().isEmpty()) return 33;
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
        graphics.pose().pushMatrix();graphics.pose().scale(1f/gameScale());
        drawPixels(graphics,preview,mc.getWindow().getWidth(),mc.getWindow().getHeight());graphics.pose().popMatrix();
    }
    public static void drawPixels(GuiGraphicsExtractor graphics,boolean preview,int width,int height){
        FpsHud.draw(graphics, width, height);
        if (Neon.config.armor.visible) {
            List<ItemStack> armor = armor(preview);
            if (!armor.isEmpty()) drawItems(graphics, armor,width,height);
        }
        if (Neon.config.totemFloating && Neon.config.totems.visible && (preview || totems() > 0)) drawTotemPixels(graphics,preview,width,height);
    }
    public static HudBounds totemBounds(int width, int height) {
        int scale=gameScale();return toGui(totemPixelBounds(width*scale,height*scale),scale);
    }
    public static HudBounds totemPixelBounds(int width,int height){
        if (Neon.config.totemFloating) return HudBounds.of(Neon.config.totems, 20, 20, width, height,density());
        var player = Minecraft.getInstance().player;
        boolean left = player == null || player.getMainArm() == HumanoidArm.RIGHT;
        int scale=gameScale();return new HudBounds(hotbarCenter(width)+(left?-120:100)*scale,((height+scale-1)/scale-22)*scale,20*scale,20*scale);
    }
    public static void drawTotem(GuiGraphicsExtractor g, boolean preview) {
        g.pose().pushMatrix();g.pose().scale(1f/gameScale());drawTotemPixels(g,preview,g.guiWidth()*gameScale(),g.guiHeight()*gameScale());g.pose().popMatrix();
    }
    public static void drawTotemPixels(GuiGraphicsExtractor g,boolean preview,int width,int height){
        if (!Neon.config.totems.visible) return;
        var b = totemPixelBounds(width,height);
        float scale = Neon.config.totemFloating ? (float) HudBounds.scale(Neon.config.totems,20,20,width,height,density()) : gameScale();
        g.pose().pushMatrix(); g.pose().translate(b.x(), b.y()); g.pose().scale(scale);
        var stack = Stacks.TOTEM_STACK;
        g.item(stack, 2, 2);
        g.itemDecorations(Minecraft.getInstance().font, stack, 2, 2, Integer.toString(preview ? Math.max(1, totems()) : totems()));
        g.pose().popMatrix();
    }
    private static void drawItems(GuiGraphicsExtractor g, List<ItemStack> items,int width,int height) {
        var font = Minecraft.getInstance().font;
        HudBounds bounds = pixelBounds(width,height,Minecraft.getInstance().gui.screen() instanceof HudEditor);
        float scale = (float) HudBounds.scale(Neon.config.armor,contentWidth(items.size()),contentHeight(items.size()),width,height,density());
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

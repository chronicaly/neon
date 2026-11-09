package me.xchro.neon.smoke;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.hud.*;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.*;
import net.minecraft.client.input.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

/** Additional 0.2.0 checks, running only in the disposable production verification client. */
final class FeatureSmoke {
    private final Minecraft mc;
    private int stage, age, checks;
    private final String profileName = "Verification " + System.currentTimeMillis();
    private UUID target;
    private boolean intact;
    private double oldZoom, armorX;
    private int oldInterval;
    FeatureSmoke(Minecraft mc, int checks) {
        this.mc = mc; this.checks = checks;
        mc.gui.setScreen(new SettingsScreen(null)); button("Camera").onPress(null);
    }
    int checks() { return checks; }
    private void check(boolean condition, String name) {
        if (!condition) throw new AssertionError("Feature stage " + stage + ": " + name);
        checks++; System.out.println("NEON_CHECK " + name);
    }
    private void next() { stage++; age = 0; }
    private List<AbstractWidget> widgets(ContainerEventHandler parent) {
        List<AbstractWidget> result = new ArrayList<>();
        for (var child : parent.children()) {
            if (child instanceof AbstractWidget widget) result.add(widget);
            if (child instanceof ContainerEventHandler container) result.addAll(widgets(container));
        }
        return result;
    }
    private List<AbstractWidget> widgets() { return widgets(mc.gui.screen()); }
    private Button button(String name) { return widgets().stream().filter(w -> w instanceof Button && w.getMessage().getString().equals(name)).map(w -> (Button) w).findFirst().orElseThrow(() -> new AssertionError("No button " + name)); }
    private Button starts(String name) { return widgets().stream().filter(w -> w instanceof Button && w.getMessage().getString().startsWith(name)).map(w -> (Button) w).findFirst().orElseThrow(); }
    private EditBox input() { return widgets().stream().filter(w -> w instanceof EditBox).map(w -> (EditBox) w).findFirst().orElseThrow(); }
    private void number(String text) { check(mc.gui.screen() instanceof NumberScreen, "exact-value dialog opens"); input().setValue(text); button("Apply").onPress(null); }
    private MouseButtonEvent mouse(double x, double y) { return new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)); }
    private void clickValue(AbstractWidget slider) { slider.mouseClicked(mouse(slider.getX() + slider.getWidth() / 2.0, slider.getY() + 10), false); }
    private void rowButton(String profile, String name) {
        int y = button(profile).getY();
        widgets().stream().filter(w -> w instanceof Button && w.getY() == y && w.getMessage().getString().equals(name)).map(w -> (Button) w).findFirst().orElseThrow().onPress(null);
    }
    private void shot(String name) { Screenshot.grab(mc.gameDirectory, "neon-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {}); }
    boolean run() {
        age++;
        switch (stage) {
            case 0 -> {
                if (age < 6) return false;
                if (age == 6) {
                    oldZoom = Neon.config.zoomFactor; armorX = Neon.config.armor.x; oldInterval = Neon.config.leftIntervalTicks;
                    var slider = widgets().stream().filter(w -> w instanceof AbstractSliderButton).findFirst().orElseThrow();
                    clickValue(slider); input().setValue("17");
                    check(!button("Apply").active, "exact value rejects out-of-range input"); return false;
                }
                if (age < 10) return false;
                if (age == 10) {
                    shot("exact-value-entry");
                    number("4.125"); check(Neon.config.zoomFactor == 4.125, "exact zoom preserves decimals between slider steps");
                    button("Undo").onPress(null); check(Neon.config.zoomFactor == oldZoom, "Undo restores the previous numeric value");
                    button("Reset page").onPress(null);
                    check(Neon.config.zoomFactor == 4 && Neon.config.leftIntervalTicks == oldInterval && Neon.config.armor.x == armorX, "page reset changes Camera only");
                    button("Undo").onPress(null); check(Neon.config.zoomFactor == oldZoom, "Undo reverses a section reset");
                    var drag = (AbstractSliderButton) widgets().stream().filter(w -> w instanceof AbstractSliderButton).findFirst().orElseThrow();
                    drag.mouseClicked(mouse(drag.getRight() - 4, drag.getY() + 10), false); drag.onRelease(mouse(drag.getRight() - 4, drag.getY() + 10));
                    return false;
                }
                if (age < 14) return false;
                check(button("Undo").active, "Undo becomes available immediately after a slider drag");
                button("Undo").onPress(null); check(Neon.config.zoomFactor == oldZoom, "one Undo restores a complete settings slider drag");
                button("Controls").onPress(null); button("Unassigned").onPress(null);
                mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_F9, 0, 0));
                button("Undo").onPress(null); check(Neon.settings.isUnbound(), "Undo restores a changed key binding");
                Neon.config.zoomFactor = 4.125;
                button("Profiles...").onPress(null); input().setValue(profileName); button("Save as...").onPress(null);
                check(Neon.profiles.names().contains(profileName), "profile can be saved through the native menu");
                Neon.config.zoomFactor = 6; button(profileName).onPress(null);
                check(Neon.config.zoomFactor == 4.125 && Neon.settings.isUnbound(), "profile load restores settings and preserves bindings");
                button("Undo settings").onPress(null); check(Neon.config.zoomFactor == 6, "profile load can be undone");
                rowButton(profileName, "Update"); button("Yes").onPress(null);
                check(Neon.profiles.get(profileName).zoomFactor == 6, "saved profile can be updated");
                rowButton(profileName, "Delete"); button("Yes").onPress(null);
                check(!Neon.profiles.names().contains(profileName), "saved profile can be deleted"); next();
            }
            case 1 -> {
                if (age < 8) return false;
                shot("profiles"); button("Done").onPress(null); button("HUD").onPress(null); button("Edit position...").onPress(null);
                HudBounds original = NeonHud.bounds(mc.gui.screen().width, mc.gui.screen().height, true);
                starts("X: ").onPress(null); number("0"); starts("Y: ").onPress(null); number("0");
                HudBounds moved = NeonHud.bounds(mc.gui.screen().width, mc.gui.screen().height, true);
                check(moved.x() == 0 && moved.y() == 0 && Neon.config.armorAnchor == NeonConfig.ArmorAnchor.CUSTOM, "exact HUD coordinates reach the top-left edge");
                button("Undo").onPress(null); button("Undo").onPress(null);
                check(NeonHud.bounds(mc.gui.screen().width, mc.gui.screen().height, true).equals(original), "HUD coordinate edits undo independently");
                button("Snap: OFF").onPress(null);
                mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_H, 0, 0));
                var editor = (HudEditor) mc.gui.screen();
                HudBounds b = NeonHud.bounds(editor.width, editor.height, true);
                editor.mouseClicked(mouse(b.x() + 4, b.y() + 4), false);
                editor.mouseDragged(mouse(editor.width - b.width() + 2, (editor.height - b.height()) / 2.0 + 6), 0, 0);
                editor.mouseReleased(mouse(0, 0));
                HudBounds snapped = NeonHud.bounds(editor.width, editor.height, true);
                check(snapped.x() + snapped.width() == editor.width && snapped.y() == Math.round((editor.height - snapped.height()) / 2.0), "HUD drag snaps to edge and center guides");
                editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z, 0, GLFW.GLFW_MOD_CONTROL));
                check(NeonHud.bounds(editor.width, editor.height, true).equals(original), "one Undo reverses a complete HUD drag");
                editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_H, 0, 0));
                double oldScale = Neon.config.armor.scale;
                editor.mouseScrolled(original.x() + 4, original.y() + 4, 0, 1);
                button("Undo").onPress(null); check(Neon.config.armor.scale == oldScale, "HUD wheel scaling can be undone");
                int actualDamage = mc.player.getItemBySlot(EquipmentSlot.HEAD).getDamageValue();
                button("Damage preview: OFF").onPress(null);
                ItemStack shown = NeonHud.armor(true).getFirst();
                check(shown.getDamageValue() == shown.getMaxDamage() - 1 && mc.player.getItemBySlot(EquipmentSlot.HEAD).getDamageValue() == actualDamage, "low-durability preview never changes real equipment"); next();
            }
            case 2 -> {
                if (age < 8) return false;
                shot("hud-editor-guides"); button("Done").onPress(null); button("Camera").onPress(null); button("Hold").onPress(null);
                var scroll = (AbstractScrollArea) widgets().stream().filter(w -> w instanceof AbstractScrollArea).findFirst().orElseThrow();
                scroll.setScrollAmount(scroll.maxScrollAmount());
                var step = widgets().stream().filter(w -> w instanceof AbstractSliderButton && w.getMessage().getString().equals("0.5x")).findFirst().orElseThrow();
                clickValue(step); number("1.25"); check(Neon.config.zoomScrollStep == 1.25 && Neon.config.zoomToggle, "zoom mode and exact scroll step are configurable"); next();
            }
            case 3 -> {
                if (age < 8) return false;
                shot("zoom-options"); button("Done").onPress(null);
                Neon.config.zoomFactor = 4; Neon.zoom.setDown(false); Neon.CAMERA.reset(mc, true); next();
            }
            case 4 -> { if (age >= 4) { Neon.zoom.setDown(true); next(); } }
            case 5 -> { if (age >= 12) { Neon.zoom.setDown(false); next(); } }
            case 6 -> {
                if (age < 4) return false;
                check(Neon.CAMERA.fov(90) < 30, "Toggle zoom remains active after releasing its key");
                check(Neon.CAMERA.scroll(2) && Neon.config.zoomFactor == 6.5, "zoom wheel uses the configured 1.25x step");
                Neon.zoom.setDown(true); next();
            }
            case 7 -> {
                if (age < 12) return false;
                check(Math.abs(Neon.CAMERA.fov(90) - 90) < .001, "second zoom toggle restores normal FOV");
                Neon.zoom.setDown(false); next();
            }
            case 8 -> { if (age >= 4) { Neon.zoom.setDown(true); next(); } }
            case 9 -> { if (age >= 12) { LiveSmoke.focused = false; next(); } }
            case 10 -> {
                if (age < 3) return false;
                check(Neon.CAMERA.fov(90) == 90, "focus loss cancels Toggle zoom"); LiveSmoke.focused = true; next();
            }
            case 11 -> {
                if (age < 4) return false;
                check(Neon.CAMERA.fov(90) == 90, "held zoom key cannot retoggle after focus loss");
                Neon.zoom.setDown(false); Neon.CLICKER.cancel(mc);
                Neon.config.clickerEnabled = Neon.config.toggleClicker = Neon.config.clickerEntitiesOnly = true;
                Neon.config.leftClick = true; Neon.config.rightClick = Neon.config.clickerProtectTools = false;
                Neon.config.leftAction = NeonConfig.LeftAction.CLICK; Neon.config.leftIntervalTicks = 1;
                Neon.autoclicker.setDown(false); mc.player.setXRot(-90); LiveSmoke.attacks.clear(); LiveSmoke.trackClicks = true; next();
            }
            case 12 -> { if (age >= 5) { Neon.autoclicker.setDown(true); next(); } }
            case 13 -> {
                if (age == 1) Neon.autoclicker.setDown(false);
                if (age < 20) return false;
                check(Neon.CLICKER.active() && LiveSmoke.attacks.isEmpty(), "entities-only mode suppresses attacks into air");
                mc.getSingleplayerServer().execute(() -> {
                    var server = mc.getSingleplayerServer(); var world = server.overworld();
                    world.setBlockAndUpdate(new BlockPos(0, 100, 0), Blocks.STONE.defaultBlockState());
                    server.getPlayerList().getPlayers().getFirst().teleportTo(.5, 101, .5);
                    ArmorStand stand = new ArmorStand(world, .5, 101, 3); stand.setInvulnerable(true); stand.setNoGravity(true);
                    world.addFreshEntity(stand); mc.execute(() -> target = stand.getUUID());
                });
                mc.player.setYRot(0); mc.player.setXRot(0); next();
            }
            case 14 -> {
                if (age < 30) return false;
                check(mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY && !LiveSmoke.attacks.isEmpty(), "entities-only mode attacks a real targeted entity");
                LiveSmoke.trackClicks = false;
                Neon.applyConfig(ConfigStore.copy(Neon.config));
                check(!Neon.CLICKER.active(), "applying profile settings immediately stops active automation"); next();
            }
            case 15 -> {
                if (age < 8) return false;
                check(!Neon.CLICKER.active(), "profile settings do not reactivate automation automatically");
                Neon.config.clickerEntitiesOnly = false; Neon.config.leftAction = NeonConfig.LeftAction.HOLD;
                Neon.config.clickerProtectTools = true; Neon.config.clickerMinDurability = 5;
                mc.player.setXRot(90); intact = false;
                mc.getSingleplayerServer().execute(() -> {
                    var server = mc.getSingleplayerServer(); var world = server.overworld();
                    if (target != null && world.getEntity(target) != null) world.getEntity(target).discard();
                    var player = server.getPlayerList().getPlayers().getFirst();
                    ItemStack shovel = new ItemStack(Items.DIAMOND_SHOVEL); shovel.setDamageValue(shovel.getMaxDamage() - 5);
                    player.getInventory().setItem(0, shovel); player.teleportTo(.5, 101, .5);
                    world.setBlockAndUpdate(new BlockPos(0, 100, 0), Blocks.DIRT.defaultBlockState());
                });
                Neon.autoclicker.setDown(false); next();
            }
            case 16 -> {
                if (age < 20) return false;
                ItemStack tool = mc.player.getMainHandItem();
                check(tool.getMaxDamage() - tool.getDamageValue() == 5, "tool-protection fixture has five durability remaining");
                Neon.autoclicker.setDown(true); next();
            }
            case 17 -> {
                if (age < 8) return false;
                check(!Neon.CLICKER.active() && Neon.CLICKER.lastStopReason().contains("5 durability"), "tool protection stops clicking with a specific action-bar reason");
                mc.getSingleplayerServer().execute(() -> {
                    boolean present = mc.getSingleplayerServer().overworld().getBlockState(new BlockPos(0, 100, 0)).is(Blocks.DIRT);
                    mc.execute(() -> intact = present);
                }); next();
            }
            case 18 -> {
                if (age < 8) return false;
                check(intact && mc.player.getMainHandItem().getMaxDamage() - mc.player.getMainHandItem().getDamageValue() == 5, "tool protection prevents mining and further tool wear");
                shot("tool-protection");
                mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().getInventory().setItem(0, new ItemStack(Items.DIAMOND_SHOVEL)));
                next();
            }
            case 19 -> {
                if (age < 20) return false;
                check(!Neon.CLICKER.active(), "replacing a protected tool while the activation key is held cannot resume clicking");
                Neon.autoclicker.setDown(false); next();
            }
            case 20 -> { if (age >= 4) { Neon.autoclicker.setDown(true); next(); } }
            case 21 -> {
                if (age < 10) return false;
                check(Neon.CLICKER.active(), "fresh activation works after replacing the protected tool");
                Neon.autoclicker.setDown(false); Neon.CLICKER.cancel(mc); Neon.config.clickerEnabled = false;
                return true;
            }
        }
        return false;
    }
}

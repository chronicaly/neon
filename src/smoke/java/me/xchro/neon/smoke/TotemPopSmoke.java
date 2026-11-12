package me.xchro.neon.smoke;

import com.mojang.authlib.GameProfile;
import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.render.TotemPopCounter;
import me.xchro.neon.ui.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.*;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.scores.Team;
import org.lwjgl.glfw.GLFW;

/** Exercises the packaged packet and nametag mixins on actual client entities. */
final class TotemPopSmoke {
    private final Minecraft mc;
    private final NeonConfig original = ConfigStore.copy(Neon.config);
    private final List<RemotePlayer> players = new ArrayList<>();
    private SettingsScreen menu;
    private int age, stage, checks, sounds;

    TotemPopSmoke(Minecraft mc, int checks) {
        this.mc = mc; this.checks = checks;
        Neon.TOTEM_POPS.clear(); Neon.config.totemPopsEnabled = true; Neon.config.totemPopsRedAt = 10;
        mc.getWindow().setWindowed(1280, 800); mc.options.guiScale().set(2); mc.resizeGui();
        mc.player.setYRot(0); mc.player.setXRot(0); if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        for (int i = 0; i < 3; i++) {
            var player = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), new String[]{"Green", "Yellow", "Red"}[i]));
            player.setId(92000 + i); player.snapTo(.5 + (i - 1) * 2.5, 101, 6, 180, 0);
            mc.level.addEntity(player); players.add(player);
        }
    }
    int checks() { return checks; }
    private void check(boolean ok, String name) { if (!ok) throw new AssertionError("Totem pops " + stage + ": " + name); checks++; System.out.println("NEON_CHECK " + name); }
    private void event(Entity entity, byte status) { mc.getConnection().handleEntityEvent(new ClientboundEntityEventPacket(entity, status)); }
    private void pop(Entity entity, int amount) { for (int i = 0; i < amount; i++) event(entity, EntityEvent.PROTECTED_FROM_DEATH); }
    private EntityRenderState state(RemotePlayer player) { return mc.getEntityRenderDispatcher().getPlayerRenderer(player).createRenderState(player, 1); }
    private int count(RemotePlayer player) { return Neon.TOTEM_POPS.count(player.getUUID()); }
    private void shot(String name) { Screenshot.grab(mc.gameDirectory, "neon-totem-pops-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {}); }
    private List<AbstractWidget> widgets(ContainerEventHandler parent) {
        var all = new ArrayList<AbstractWidget>();
        for (var child : parent.children()) { if (child instanceof AbstractWidget w) all.add(w); if (child instanceof ContainerEventHandler c) all.addAll(widgets(c)); }
        return all;
    }
    private UiButton button(String label) { return widgets(menu).stream().filter(w -> w instanceof UiButton && w.getMessage().getString().equals(label)).map(w -> (UiButton) w).findFirst().orElseThrow(); }
    private MouseButtonEvent pointer(double x, double y) { return new MouseButtonEvent(menu.guiCoordinate(x), menu.guiCoordinate(y), new MouseButtonInfo(0, 0)); }
    private void click(AbstractWidget widget) {
        var e = pointer(widget.getX() + widget.getWidth() / 2.0, widget.getY() + widget.getHeight() / 2.0);
        check(menu.mouseClicked(e, false), "new module accepts native pointer input"); menu.mouseReleased(e);
    }
    private void openMenu() {
        new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"), Neon.SAVES); Neon.SAVES.flush().join();
        mc.gui.setScreen(menu = Menus.fresh(null)); click(button("Totem pops"));
    }
    boolean run() {
        // Let the real totem particles expire before capturing all three nametags.
        if (++age < (stage == 1 ? 80 : 15)) return false; age = 0;
        var green = players.get(0); var yellow = players.get(1); var red = players.get(2);
        switch (stage++) {
            case 0 -> {
                check(state(green).nameTag.getString().equals("Green"), "no zero suffix before the first pop");
                event(green, (byte) 0); check(count(green) == 0, "unrelated status is ignored");
                var mob = new ArmorStand(mc.level, 8, 101, 6); mob.setId(92999); mc.level.addEntity(mob);
                pop(mob, 1); check(Neon.TOTEM_POPS.count(mob.getUUID()) == 0, "non-player totem events are ignored");
                mc.level.removeEntity(mob.getId(), Entity.RemovalReason.DISCARDED); pop(mob, 1);
                check(count(green) == 0, "unknown entity events are harmless");
                pop(green, 1); pop(yellow, 5); pop(red, 10);
            }
            case 1 -> {
                for (int i = 0; i < 3; i++) {
                    var player = players.get(i); int expected = new int[]{1, 5, 10}[i];
                    var tag = state(player).nameTag;
                    check(count(player) == expected && tag.getString().equals(player.getName().getString() + " -" + expected), "packaged packet/render hooks count each pop once for " + player.getName().getString());
                    check(tag.getSiblings().getLast().getStyle().getColor().getValue() == TotemPopCounter.color(expected, 10), "suffix uses the expected green-to-red color");
                    check(player.getDisplayName().getString().equals(player.getName().getString()), "nametag decoration never mutates player/tab/chat names");
                }
                check(TotemPopCounter.color(1, 10) == 0x55ff55 && TotemPopCounter.color(10, 10) == 0xff5555, "vanilla green and red endpoints");
                shot("nametags");
            }
            case 2 -> {
                pop(red, 3); check(state(red).nameTag.getString().endsWith(" -13"), "count continues after the color reaches red");
                var team = mc.level.getScoreboard().addPlayerTeam("pop-verification");
                team.setColor(Optional.of(net.minecraft.world.scores.TeamColor.AQUA)); team.setPlayerPrefix(Component.literal("[Team] ").withStyle(ChatFormatting.BOLD));
                mc.level.getScoreboard().addPlayerToTeam(green.getScoreboardName(), team);
                var tag = state(green).nameTag;
                check(tag.getString().equals("[Team] Green -1") && tag.getSiblings().getFirst().equals(green.getDisplayName()), "team prefix and styling are preserved");
                check(!tag.getSiblings().getLast().getStyle().isBold(), "counter does not inherit team bold styling");
                team.setNameTagVisibility(Team.Visibility.NEVER); check(state(green).nameTag == null, "hidden team nametags stay hidden");
                mc.level.getScoreboard().removePlayerTeam(team);
                mc.gui.hud.toggle(); check(state(green).nameTag == null, "F1 hides the vanilla nametag and counter together"); mc.gui.hud.toggle();
                green.setInvisible(true); check(state(green).nameTag == null, "invisible players stay hidden"); green.setInvisible(false);
                green.snapTo(.5, 101, 100); check(state(green).nameTag == null, "vanilla nametag distance is retained"); green.snapTo(-2, 101, 6);
                Neon.config.totemPopsEnabled = false; pop(green, 1);
                check(state(green).nameTag.getString().equals("Green") && count(green) == 2, "disabled display stays hidden while tracking continues"); Neon.config.totemPopsEnabled = true;
                check(state(green).nameTag.getString().equals("Green -2"), "reenabling restores the current count without duplicate suffixes");
            }
            case 3 -> {
                event(green, EntityEvent.DEATH); check(count(green) == 0, "death packet resets only that player's count");
                check(count(yellow) == 5, "other players retain independent counts");
                yellow.setHealth(0); Neon.TOTEM_POPS.tick(mc.level); check(count(yellow) == 0, "health-based death also clears the count");
                mc.getConnection().handlePlayerInfoRemove(new ClientboundPlayerInfoRemovePacket(List.of(red.getUUID())));
                check(count(red) == 0, "player departure clears its count");
                pop(red, 2); Neon.TOTEM_POPS.tick(null); check(count(red) == 0, "leaving the world clears all counters");
                Neon.TOTEM_POPS.tick(mc.level); pop(red, 1); check(count(red) == 1, "new world context starts at minus one");
                Neon.TOTEM_POPS.clear(); check(count(red) == 0, "disconnect cleanup clears counts");
                for (var player : players) mc.level.removeEntity(player.getId(), Entity.RemovalReason.DISCARDED);
                var loaded = ConfigStore.decode("{}"); check(loaded.totemPopsEnabled && loaded.totemPopsRedAt == 10, "legacy configurations enable the new module with its default threshold");
                loaded.totemPopsEnabled = false; loaded.totemPopsRedAt = 7; loaded = ConfigStore.copy(loaded);
                check(!loaded.totemPopsEnabled && loaded.totemPopsRedAt == 7, "configuration snapshots preserve module preferences");
                openMenu();
            }
            case 4 -> {
                shot("default"); sounds = SoundAudit.neonEnables;
                var toggle = widgets(menu).stream().filter(w -> w instanceof UiButton b && b.hint().equals("Enable Totem pops")).findFirst().orElseThrow();
                click(toggle); check(!Neon.config.totemPopsEnabled && SoundAudit.neonEnables == sounds, "disabling the new module never requests an enable cue");
                click(toggle); check(Neon.config.totemPopsEnabled && SoundAudit.neonEnables == sounds + 1, "enabling requests one NXE enable cue");
                var slider = widgets(menu).stream().filter(w -> w instanceof ExactSlider).map(w -> (ExactSlider) w).findFirst().orElseThrow();
                var e = pointer(slider.getRight() - 2, slider.getY() + slider.getHeight() / 2.0); menu.mouseClicked(e, false); menu.mouseReleased(e);
                check(slider.isEditing(), "Red at edits inline"); InlineInput.field(menu).setValue("7"); menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
                check(Neon.config.totemPopsRedAt == 7, "threshold applies immediately");
                menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z, 0, GLFW.GLFW_MOD_CONTROL)); check(Neon.config.totemPopsRedAt == 10, "threshold editing supports Undo");
                var b = menu.menuBounds(); var from = pointer(b.right() - 1, b.bottom() - 1); var to = pointer(b.right() - 121, b.bottom() - 71);
                menu.mouseClicked(from, false); menu.mouseDragged(to, to.x() - from.x(), to.y() - from.y()); menu.mouseReleased(to);
            }
            case 5 -> { shot("minimum"); check(menu.menuBounds().width() == 320, "new module works at minimum menu width"); }
            default -> { Neon.applyConfig(original); mc.gui.setScreen(null); return true; }
        }
        return false;
    }
}

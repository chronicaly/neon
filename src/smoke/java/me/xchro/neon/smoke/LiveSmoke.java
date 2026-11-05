package me.xchro.neon.smoke;

import me.xchro.neon.Neon;
import me.xchro.neon.hud.NeonHud;
import me.xchro.neon.mixin.KeyAccessor;
import me.xchro.neon.ui.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.*;
import net.minecraft.client.input.*;
import me.xchro.neon.config.NeonConfig.ArmorAnchor;
import org.lwjgl.glfw.GLFW;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.nio.file.*;
import java.util.*;

public final class LiveSmoke implements ClientModInitializer {
    public static boolean focused = true;
    public static boolean trackClicks;
    public static int currentTick;
    public static final List<Integer> attacks = new ArrayList<>(), uses = new ArrayList<>();
    public static final List<Integer> useDelays = new ArrayList<>();
    public static final List<Float> strengths = new ArrayList<>();
    private int lastAttacks, lastUses;
    private int tick, stageTick, stage, checks;
    private float yaw, pitch;
    private boolean blockBroken;
    private boolean initialized, failed;
    private Minecraft mc;
    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("neon.smoke")) throw new IllegalStateException("Test mod outside verification run");
        Neon.config = new me.xchro.neon.config.NeonConfig();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }
    private void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        checks++; System.out.println("NEON_CHECK " + name);
    }
    private void next() { stage++; stageTick = tick; }
    private void shot(String name) { Screenshot.grab(mc.gameDirectory, "neon-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {}); }
    private List<AbstractWidget> widgets(ContainerEventHandler parent) {
        List<AbstractWidget> result = new ArrayList<>();
        for (var child : parent.children()) {
            if (child instanceof AbstractWidget widget) result.add(widget);
            if (child instanceof ContainerEventHandler container) result.addAll(widgets(container));
        }
        return result;
    }
    private Button button(String label) {
        return widgets(mc.gui.screen()).stream().filter(w -> w instanceof Button && w.getMessage().getString().equals(label)).map(w -> (Button) w).findFirst().orElseThrow();
    }
    private void tick(Minecraft client) {
        mc = client; tick++;
        if (failed) return;
        try {
            if (tick > 3600) throw new AssertionError("Timed out at stage " + stage + ", screen=" + mc.gui.screen());
            if (mc.player == null || mc.level == null) return;
            if (!initialized) {
                initialized = true; stageTick = tick;
                mc.options.pauseOnLostFocus = false;
                mc.options.renderDistance().set(6);
                mc.options.simulationDistance().set(5);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.gui.setScreen(null);
                var server = mc.getSingleplayerServer();
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.SURVIVAL);
                    var world = player.level();
                    for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
                        world.setBlockAndUpdate(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState());
                        for (int y = 101; y < 108; y++) world.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                    player.teleportTo(0.5, 101, .5);
                    player.setHealth(20);
                    player.getFoodData().setFoodLevel(20);
                    player.getInventory().clearContent();
                    player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                    ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE); chest.setDamageValue(100);
                    player.setItemSlot(EquipmentSlot.CHEST, chest);
                    player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                    player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
                    player.getInventory().setItem(0, new ItemStack(Items.TOTEM_OF_UNDYING));
                    player.getInventory().setItem(1, new ItemStack(Items.TOTEM_OF_UNDYING));
                    player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
                });
                return;
            }
            int age = tick - stageTick;
            switch (stage) {
                case 0 -> {
                    if (age < 80 || mc.gui.screen() != null) return;
                    check(NeonHud.totems() == 3, "inventory and offhand total, main hand counted once");
                    check("3".equals(NeonHud.offhandCount(mc.player.getOffhandItem())), "offhand renders total count");
                    check(NeonHud.offhandCount(mc.player.getMainHandItem()) == null, "main-hand totem keeps vanilla count");
                    check(NeonHud.armor(false).size() == 4, "four equipped armor items");
                    check(!Neon.CLICKER.active(), "autoclicker starts inactive");
                    mc.player.setYRot(0); mc.player.setXRot(0); yaw = mc.player.getYRot(); pitch = mc.player.getXRot();
                    Neon.freelook.setDown(true); next();
                }
                case 1 -> {
                    if (age == 1) { Neon.CAMERA.frame(mc); check(Neon.CAMERA.looking(), "freelook enters immediately"); }
                    if (age < 12) return;
                    check(Neon.CAMERA.looking(), "held freelook activates");
                    check(mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK, "freelook uses vanilla third person");
                    Neon.CAMERA.turn(400, -100);
                    check(mc.player.getYRot() == yaw && mc.player.getXRot() == pitch, "orbit preserves player aim");
                    Neon.zoom.setDown(true); next();
                }
                case 2 -> {
                    if (age < 12) return;
                    check(Neon.CAMERA.fov(90) < 30, "four-times zoom during freelook");
                    check(Neon.CAMERA.scroll(2), "active zoom consumes wheel");
                    check(Neon.config.zoomFactor == 5, "wheel adjusts magnification");
                    shot("freelook-zoom-hud"); Neon.freelook.setDown(false); next();
                }
                case 3 -> {
                    if (age == 1) { Neon.CAMERA.frame(mc); check(!Neon.CAMERA.ownsCamera(), "release restores perspective immediately"); }
                    if (age < 12) return;
                    check(mc.options.getCameraType() == CameraType.FIRST_PERSON, "release restores first person");
                    check(!Neon.CAMERA.ownsCamera(), "release clears camera ownership");
                    Neon.zoom.setDown(false); next();
                }
                case 4 -> {
                    if (age < 12) return;
                    check(Math.abs(Neon.CAMERA.fov(90) - 90) < .001, "zoom fully returns to normal");
                    check(!Neon.CAMERA.scroll(1), "inactive zoom does not consume wheel");
                    mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); Neon.freelook.setDown(true); next();
                }
                case 5 -> {
                    if (age < 10) return;
                    check(mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT, "front perspective preserved");
                    mc.options.setCameraType(CameraType.FIRST_PERSON); next();
                }
                case 6 -> {
                    if (age < 3) return;
                    check(!Neon.CAMERA.ownsCamera(), "manual perspective cancels freelook");
                    check(mc.options.getCameraType() == CameraType.FIRST_PERSON, "manual perspective wins");
                    Neon.freelook.setDown(false); next();
                }
                case 7 -> {
                    if (age < 3) return;
                    Neon.freelook.setDown(true); Neon.zoom.setDown(true); next();
                }
                case 8 -> {
                    if (age < 8) return;
                    focused = false; next();
                }
                case 9 -> {
                    if (age < 3) return;
                    check(!Neon.CAMERA.ownsCamera(), "focus loss restores camera");
                    check(Neon.CAMERA.fov(90) == 90, "focus loss clears zoom");
                    focused = true; next();
                }
                case 10 -> {
                    if (age < 4) return;
                    check(!Neon.CAMERA.looking(), "held key cannot reactivate after focus loss");
                    Neon.freelook.setDown(false); Neon.zoom.setDown(false);
                    Neon.config.clickerEnabled = true; next();
                }
                case 11 -> { if (age >= 3) { Neon.autoclicker.setDown(true); next(); } }
                case 12 -> {
                    if (age < 5) return;
                    check(Neon.CLICKER.active(), "autoclicker hold activates");
                    check(!mc.options.keyAttack.isDown(), "synthetic attack does not stick");
                    check(((KeyAccessor)mc.options.keyAttack).neon$getClicks() == 0, "synthetic click queue does not leak");
                    mc.gui.setScreen(new SettingsScreen(null)); next();
                }
                case 13 -> {
                    if (age < 8) return;
                    check(!Neon.CLICKER.active(), "settings screen stops clicking");
                    shot("settings"); mc.gui.setScreen(null); next();
                }
                case 14 -> {
                    if (age < 4) return;
                    check(!Neon.CLICKER.active(), "held activation cannot resume after menu");
                    Neon.autoclicker.setDown(false); Neon.config.clickerEnabled = false;
                    mc.gui.setScreen(new HudEditor(new SettingsScreen(null))); next();
                }
                case 15 -> {
                    if (age < 8) return;
                    shot("hud-editor");
                    Neon.config.armorVertical = true; Neon.config.armor.x = 1; Neon.config.armor.y = 1;
                    var bounds = NeonHud.bounds(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), true);
                    check(bounds.height() > bounds.width(), "armor can be vertical");
                    check(bounds.x() + bounds.width() == mc.getWindow().getGuiScaledWidth()
                        && bounds.y() + bounds.height() == mc.getWindow().getGuiScaledHeight(), "armor touches bottom and right edges without padding");
                    var chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
                    chest.setDamageValue(chest.getMaxDamage() - 2);
                    check(NeonHud.lowDurability(chest), "low armor durability warning");
                    check(!NeonHud.lowDurability(mc.player.getItemBySlot(EquipmentSlot.HEAD)), "healthy armor has no warning");
                    check(Neon.save(), "settings save succeeds");
                    mc.gui.setScreen(null); next();
                }
                case 16 -> {
                    if (age < 8) return;
                    shot("vanilla-hud");
                    mc.player.setYRot(0); mc.player.setXRot(0);
                    mc.getSingleplayerServer().execute(() -> {
                        var world = mc.getSingleplayerServer().overworld();
                        for (int x = -4; x <= 4; x++) for (int y = 101; y <= 105; y++)
                            world.setBlockAndUpdate(new BlockPos(x, y, -1), Blocks.STONE.defaultBlockState());
                    });
                    next();
                }
                case 17 -> { if (age >= 10) { Neon.freelook.setDown(true); next(); } }
                case 18 -> {
                    if (age < 12) return;
                    double cameraDistance = mc.gameRenderer.mainCamera().position().distanceTo(mc.player.getEyePosition());
                    check(cameraDistance < 1.7, "freelook camera respects nearby wall collision");
                    check(Neon.CAMERA.looking(), "collision test has active freelook");
                    shot("camera-wall"); Neon.freelook.setDown(false); next();
                }
                case 19 -> {
                    if (age < 2) return;
                    Neon.CAMERA.frame(mc);
                    check(!Neon.CAMERA.ownsCamera(), "rapid release immediately clears camera");
                    Neon.freelook.setDown(true); Neon.CAMERA.frame(mc);
                    check(Neon.CAMERA.looking() && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK,
                        "rapid repress immediately restarts freelook");
                    next();
                }
                case 20 -> {
                    if (age < 10) return;
                    Neon.freelook.setDown(false);
                    mc.getSingleplayerServer().execute(() -> {
                        var server = mc.getSingleplayerServer(); var world = server.overworld();
                        for (int x = -4; x <= 4; x++) for (int y = 101; y <= 105; y++)
                            world.setBlockAndUpdate(new BlockPos(x, y, -1), Blocks.AIR.defaultBlockState());
                        var player = server.getPlayerList().getPlayers().getFirst();
                        player.teleportTo(.5, 101, .5);
                        player.getInventory().setSelectedSlot(0);
                        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SHOVEL));
                        world.setBlockAndUpdate(new BlockPos(0, 100, 0), Blocks.DIRT.defaultBlockState());
                        world.setBlockAndUpdate(new BlockPos(0, 99, 0), Blocks.BEDROCK.defaultBlockState());
                    });
                    mc.player.setXRot(90); Neon.config.clickerEnabled = true; next();
                }
                case 21 -> { if (age >= 15) { Neon.autoclicker.setDown(true); next(); } }
                case 22 -> {
                    if (age < 30) return;
                    mc.getSingleplayerServer().execute(() -> {
                        boolean broken = mc.getSingleplayerServer().overworld().getBlockState(new BlockPos(0, 100, 0)).isAir();
                        mc.execute(() -> blockBroken = broken);
                    });
                    Neon.autoclicker.setDown(false); next();
                }
                case 23 -> {
                    if (age < 10) return;
                    check(blockBroken, "continuous mining breaks a real server block");
                    check(!mc.gameMode.isDestroying(), "release cancels continuous mining");
                    mc.options.keyAttack.setDown(true); next();
                }
                case 24 -> {
                    if (age < 2) return;
                    check(mc.options.keyAttack.isDown(), "normal held attack input is preserved");
                    mc.options.keyAttack.setDown(false); Neon.config.clickerEnabled = false;
                    mc.gui.setScreen(new SettingsScreen(null));
                    ((net.minecraft.client.gui.components.Button)mc.gui.screen().children().getFirst()).onPress(null);
                    next();
                }
                case 25 -> {
                    if (age < 8) return;
                    shot("camera-settings"); mc.gui.setScreen(new SettingsScreen(null));
                    ((net.minecraft.client.gui.components.Button)mc.gui.screen().children().get(1)).onPress(null); next();
                }
                case 26 -> {
                    if (age < 8) return;
                    shot("autoclicker-settings"); button("HUD").onPress(null); next();
                }
                case 27 -> {
                    if (age < 8) return;
                    shot("hud-settings");
                    mc.gui.screen().resize(320, 240);
                    check(widgets(mc.gui.screen()).stream().filter(w -> w instanceof AbstractScrollArea)
                        .allMatch(w -> w.getY() >= 59 && w.getBottom() <= 195 && w.getX() >= 0 && w.getRight() <= 320), "settings viewport fits minimum GUI size");
                    var scroll = (AbstractScrollArea) widgets(mc.gui.screen()).stream().filter(w -> w instanceof AbstractScrollArea).findFirst().orElseThrow();
                    scroll.setScrollAmount(scroll.maxScrollAmount());
                    check(widgets(mc.gui.screen()).stream().anyMatch(w -> w.getMessage().getString().equals("Offhand total") && w.getY() >= scroll.getY()
                        && w.getBottom() <= scroll.getBottom()), "last HUD setting is reachable by scrolling");
                    button("Controls").onPress(null);
                    button("Unassigned").onPress(null);
                    mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_F8, 0, 0));
                    check(Neon.settings.matches(new KeyEvent(GLFW.GLFW_KEY_F8, 0, 0)), "settings key assigns directly in config");
                    button("F8").onPress(null);
                    mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE, 0, 0));
                    check(mc.gui.screen() instanceof SettingsScreen && Neon.settings.matches(new KeyEvent(GLFW.GLFW_KEY_F8, 0, 0)), "escape cancels key capture without closing config");
                    button("F8").onPress(null);
                    mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_BACKSPACE, 0, 0));
                    check(Neon.settings.isUnbound(), "backspace clears assigned key");
                    mc.gui.setScreen(null);
                    Neon.config.armorAnchor = ArmorAnchor.HOTBAR_LEFT; Neon.config.armorVertical = false;
                    var b = NeonHud.bounds(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), false);
                    check(b.x() + b.width() <= mc.getWindow().getGuiScaledWidth() / 2 - 122, "hotbar armor makes room for offhand slot");
                    Neon.config.toggleClicker = true; Neon.config.clickerEnabled = true;
                    Neon.config.continuousMining = false; Neon.config.leftCps = Neon.config.rightCps = 20;
                    Neon.config.rightClick = true; mc.player.setXRot(-90);
                    mc.options.pauseOnLostFocus = true;
                    next();
                }
                case 28 -> {
                    if (age < 6) return;
                    attacks.clear(); uses.clear(); strengths.clear(); useDelays.clear(); trackClicks = true;
                    Neon.autoclicker.setDown(true); next();
                }
                case 29 -> {
                    if (age < 20) return;
                    Neon.autoclicker.setDown(false);
                    check(Neon.CLICKER.active(), "toggle starts before chat");
                    lastAttacks = attacks.size(); lastUses = uses.size();
                    mc.gui.setScreen(new ChatScreen("", false));
                    for (char c : "typing while clicking".toCharArray()) mc.gui.screen().charTyped(new CharacterEvent(c));
                    next();
                }
                case 30 -> {
                    if (age < 55) return;
                    check(Neon.CLICKER.active() && attacks.size() > lastAttacks && uses.size() > lastUses, "normal attacks and uses continue in chat");
                    check(widgets(mc.gui.screen()).stream().filter(w -> w instanceof EditBox).map(w -> ((EditBox) w).getValue())
                        .anyMatch(v -> v.equals("typing while clicking")), "chat input remains intact and unsent");
                    shot("chat-autoclicker"); focused = false;
                    lastAttacks = attacks.size(); lastUses = uses.size(); next();
                }
                case 31 -> {
                    if (age < 55) return;
                    check(Neon.CLICKER.active() && !mc.isPaused() && mc.gui.screen() instanceof ChatScreen, "tabbed-out chat stays active without automatic pause");
                    check(attacks.size() > lastAttacks && uses.size() > lastUses, "chat clicking continues while unfocused");
                    mc.gui.setScreen(null); lastAttacks = attacks.size(); lastUses = uses.size(); next();
                }
                case 32 -> {
                    if (age < 55) return;
                    check(Neon.CLICKER.active() && !mc.isPaused() && mc.gui.screen() == null, "tabbed-out gameplay stays active");
                    check(attacks.size() > lastAttacks && uses.size() > lastUses, "background clicking uses normal game input");
                    check(strengths.size() > 3 && strengths.stream().allMatch(v -> v >= 1), "every automatic attack waits for full vanilla weapon cooldown");
                    boolean useDelay = uses.size() > 5 && useDelays.stream().allMatch(v -> v == 0);
                    for (int i = 1; i < uses.size(); i++) useDelay &= uses.get(i) - uses.get(i - 1) >= 4;
                    if (!useDelay) System.out.println("NEON_USE_TIMING ticks=" + uses + " delays=" + useDelays);
                    check(useDelay, "right click always respects vanilla four-tick use delay");
                    focused = true; trackClicks = false;
                    Neon.config.leftClick = Neon.config.rightClick = false; Neon.config.continuousMining = true;
                    mc.player.setXRot(90); blockBroken = false;
                    mc.getSingleplayerServer().execute(() -> {
                        var server = mc.getSingleplayerServer();
                        server.getPlayerList().getPlayers().getFirst().teleportTo(.5, 101, .5);
                        server.overworld().setBlockAndUpdate(new BlockPos(0, 100, 0), Blocks.DIRT.defaultBlockState());
                    });
                    next();
                }
                case 33 -> {
                    if (age < 15) return;
                    focused = false; Neon.config.leftClick = true;
                    mc.gui.setScreen(new ChatScreen("mining test - unsent", false)); next();
                }
                case 34 -> {
                    if (age < 30) return;
                    mc.getSingleplayerServer().execute(() -> {
                        boolean broken = mc.getSingleplayerServer().overworld().getBlockState(new BlockPos(0, 100, 0)).isAir();
                        mc.execute(() -> blockBroken = broken);
                    });
                    next();
                }
                case 35 -> {
                    if (age < 8) return;
                    check(blockBroken && Neon.CLICKER.active(), "continuous mining breaks a real block while typing and unfocused");
                    focused = true; Neon.config.clickerInChat = false; next();
                }
                case 36 -> {
                    if (age < 5) return;
                    check(!Neon.CLICKER.active(), "disabling chat continuation stops autoclicker in chat");
                    Neon.autoclicker.setDown(true); next();
                }
                case 37 -> {
                    if (age < 5) return;
                    check(!Neon.CLICKER.active(), "typing an activation key in chat cannot start autoclicker");
                    Neon.autoclicker.setDown(false); mc.gui.setScreen(null);
                    Neon.config.clickerEnabled = false; Neon.config.clickerInChat = true;
                    Files.writeString(mc.gameDirectory.toPath().resolve("neon-verification.txt"), "PASS " + checks + " live checks\n");
                    System.out.println("NEON_LIVE_PASS checks=" + checks);
                    next();
                }
                case 38 -> { if (age >= 20) mc.stop(); }
            }
        } catch (Throwable error) {
            failed = true; System.err.println("NEON_LIVE_FAIL stage=" + stage); error.printStackTrace();
            try { Files.writeString(mc.gameDirectory.toPath().resolve("neon-verification.txt"), "FAIL " + error); } catch (Exception ignored) {}
            mc.stop();
        }
    }
}

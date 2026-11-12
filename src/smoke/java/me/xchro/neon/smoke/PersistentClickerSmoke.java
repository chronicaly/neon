package me.xchro.neon.smoke;

import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;
import me.xchro.neon.Neon;
import me.xchro.neon.config.NeonConfig;
import me.xchro.neon.mixin.KeyAccessor;
import me.xchro.neon.ui.SettingsScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Isolated production checks for continuous world input behind arbitrary screens. */
public final class PersistentClickerSmoke {
    private Minecraft mc;
    private int stage, age, checks, screenIndex;
    private boolean initialized, done, mined, vanillaTiming;
    private long worldTime;
    private Screen shown;
    private List<Supplier<Screen>> screens;
    private ChestMenu container;
    private ProbeScreen probe;
    private UUID target;
    public void start() { ClientTickEvents.END_CLIENT_TICK.register(this::tick); }
    private void check(boolean condition, String name) {
        if (!condition) throw new AssertionError("Persistent clicker stage " + stage + ": " + name);
        checks++; System.out.println("NEON_CHECK " + name);
    }
    private void next() { stage++; age = 0; }
    private void clear() { LiveSmoke.attacks.clear(); LiveSmoke.uses.clear(); LiveSmoke.strengths.clear(); LiveSmoke.useDelays.clear(); ClickerPacketOrder.clear(); }
    private void checkPacketOrder(String context) {
        check(ClickerPacketOrder.boundaries > 0 && ClickerPacketOrder.actions > 0,
            context + ": observed outgoing actions and tick boundaries");
        check(ClickerPacketOrder.failure == null,
            context + ": actions precede movement/tick-end" + (ClickerPacketOrder.failure == null ? "" : " (" + ClickerPacketOrder.failure + ")"));
    }
    private boolean interval(List<Integer> ticks, int expected) {
        if (ticks.size() < 5) return false;
        for (int i = 1; i < ticks.size(); i++) if (ticks.get(i) - ticks.get(i-1) != expected) return false;
        return true;
    }
    private void showNext() {
        shown = screens.get(screenIndex++).get(); mc.gui.setScreen(shown);
        clear(); age = 0; worldTime = mc.level.getGameTime();
    }
    private void tick(Minecraft client) {
        mc = client;
        if (done) return;
        try {
            if (++age > 1200) throw new AssertionError("Timed out at stage " + stage);
            if (!initialized) {
                if (mc.player == null || mc.level == null) return;
                initialized = true; age = 0;
                mc.options.pauseOnLostFocus = true; mc.options.renderDistance().set(6); mc.options.simulationDistance().set(5);
                mc.gui.setScreen(null); mc.player.setXRot(-90);
                Neon.config = new NeonConfig(); Neon.config.clickerEnabled = Neon.config.toggleClicker = true;
                // Use a real assigned binding; an unbound module cannot start from a key press.
                Neon.autoclicker.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(org.lwjgl.glfw.GLFW.GLFW_KEY_F8));
                KeyMapping.resetMapping();
                Neon.config.leftClick = Neon.config.rightClick = true;
                Neon.config.leftIntervalTicks = 2; Neon.config.rightIntervalTicks = 3;
                Neon.autoclicker.setDown(false); Neon.CLICKER.cancel(mc); LiveSmoke.trackClicks = true;
                ClickerPacketOrder.enabled = true;
                mc.getSingleplayerServer().execute(() -> {
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.SURVIVAL); player.setHealth(20); player.getFoodData().setFoodLevel(20);
                    player.getInventory().clearContent(); player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SHOVEL));
                    for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                        player.level().setBlockAndUpdate(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState());
                        for (int y = 101; y <= 105; y++) player.level().setBlockAndUpdate(new BlockPos(x,y,z), Blocks.AIR.defaultBlockState());
                    }
                    player.teleportTo(.5, 101, .5);
                });
                screens = List.of(() -> new PauseScreen(true), () -> new OptionsScreen(new PauseScreen(true), mc.options, false),
                    () -> new InventoryScreen(mc.player), () -> {
                        container = ChestMenu.threeRows(99, mc.player.getInventory());
                        container.getSlot(0).set(new ItemStack(Items.STONE,17)); container.setCarried(new ItemStack(Items.DIAMOND,3));
                        return new ContainerScreen(container, mc.player.getInventory(), Component.literal("Input isolation"));
                    }, () -> new ChatScreen("Do not type or click here", false), () -> Menus.fresh(null),
                    () -> { probe = new ProbeScreen(); return probe; });
                return;
            }
            switch (stage) {
                case 0 -> { if (age >= 60) { mc.player.setXRot(-90); Neon.autoclicker.setDown(true); next(); } }
                case 1 -> {
                    if (age < 5) return;
                    Neon.autoclicker.setDown(false); check(Neon.CLICKER.active(), "toggle activates from gameplay");
                    showNext(); next();
                }
                case 2 -> {
                    if (age < 30) return;
                    String name = shown.getClass().getSimpleName();
                    check(Neon.CLICKER.active() && mc.gui.screen() == shown, name + ": autoclicker stays on and leaves the screen open");
                    check(!mc.isPaused() && !mc.getSingleplayerServer().isPaused() && mc.level.getGameTime() > worldTime,
                        name + ": client and singleplayer world keep ticking");
                    check(interval(LiveSmoke.attacks,2) && interval(LiveSmoke.uses,3), name + ": independent attack/use tick timing continues");
                    checkPacketOrder(name);
                    check(!mc.options.keyAttack.isDown() && !mc.options.keyUse.isDown()
                        && ((KeyAccessor)mc.options.keyAttack).neon$getClicks() == 0 && ((KeyAccessor)mc.options.keyUse).neon$getClicks() == 0,
                        name + ": no synthetic physical-button or key-queue leakage");
                    if (container != null) check(container.getSlot(0).getItem().getCount()==17 && container.getCarried().getCount()==3,
                        name + ": inventory and carried stacks are untouched");
                    if (probe != null) check(probe.clicks == 0 && probe.input.getValue().equals("Unchanged text"),
                        "menu buttons and text fields receive no synthetic input");
                    if (screenIndex < screens.size()) { showNext(); return; }
                    LiveSmoke.focused = false; clear(); next();
                }
                case 3 -> {
                    if (age < 30) return;
                    if (!vanillaTiming) {
                        check(Neon.CLICKER.active() && !mc.isPaused() && interval(LiveSmoke.attacks,2) && interval(LiveSmoke.uses,3),
                            "background menu keeps both click timers and the world running");
                        checkPacketOrder("background menu with custom intervals");
                        Neon.config.leftIntervalTicks = Neon.config.rightIntervalTicks = 0;
                        Neon.CLICKER.resetTiming(); clear(); vanillaTiming = true; age = 0; return;
                    }
                    if (age < 80) return;
                    check(Neon.CLICKER.active() && LiveSmoke.strengths.size() >= 2 && LiveSmoke.strengths.stream().allMatch(v -> v >= 1),
                        "vanilla attacks wait for full weapon recharge behind a background menu");
                    boolean vanillaUse = LiveSmoke.uses.size() >= 10;
                    for (int i = 1; i < LiveSmoke.uses.size(); i++) vanillaUse &= LiveSmoke.uses.get(i)-LiveSmoke.uses.get(i-1) >= 4;
                    check(vanillaUse && LiveSmoke.useDelays.stream().allMatch(v -> v == 0),
                        "vanilla use delay remains intact behind a background menu");
                    checkPacketOrder("background menu with vanilla cooldowns");
                    Neon.config.leftIntervalTicks = 2; Neon.config.rightIntervalTicks = 3;
                    LiveSmoke.focused = true; mc.gui.setScreen(new PauseScreen(true)); Neon.config.clickerEnabled = false; clear(); next();
                }
                case 4 -> {
                    if (age < 8) return;
                    check(!Neon.CLICKER.active() && mc.isPaused() && mc.getSingleplayerServer().isPaused(),
                        "explicit disable restores normal Escape-menu pausing");
                    check(LiveSmoke.attacks.isEmpty() && LiveSmoke.uses.isEmpty(), "explicit disable stops both buttons immediately");
                    Neon.config.clickerEnabled = true; Neon.autoclicker.setDown(true); next();
                }
                case 5 -> {
                    if (age < 6) return;
                    check(!Neon.CLICKER.active(), "enabling the module or typing its key in a menu does not start a session");
                    Neon.autoclicker.setDown(false); mc.gui.setScreen(null); next();
                }
                case 6 -> { if (age >= 5) { Neon.autoclicker.setDown(true); next(); } }
                case 7 -> {
                    if (age < 5) return;
                    Neon.autoclicker.setDown(false); mc.gui.setScreen(new ChatScreen("Typing the activation key",false)); next();
                }
                case 8 -> { if (age >= 5) { Neon.autoclicker.setDown(true); next(); } }
                case 9 -> {
                    if (age < 8) return;
                    check(Neon.CLICKER.active(), "typing the activation key does not stop an active session");
                    mc.gui.setScreen(null); next();
                }
                case 10 -> {
                    if (age < 5) return;
                    check(Neon.CLICKER.active(), "closing chat does not replay the typed activation key");
                    Neon.autoclicker.setDown(false); next();
                }
                case 11 -> { if (age >= 5) { Neon.autoclicker.setDown(true); next(); } }
                case 12 -> {
                    if (age < 5) return;
                    check(!Neon.CLICKER.active(), "explicit gameplay toggle stops the session");
                    Neon.autoclicker.setDown(false); Neon.config.rightClick = false; Neon.config.leftAction = NeonConfig.LeftAction.HOLD;
                    mc.getSingleplayerServer().execute(() -> {
                        var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                        ItemStack tool = new ItemStack(Items.DIAMOND_SHOVEL); tool.setDamageValue(tool.getMaxDamage()-5);
                        player.getInventory().setItem(0,tool); player.teleportTo(.5,101,.5);
                        player.level().setBlockAndUpdate(new BlockPos(0,100,0), Blocks.DIRT.defaultBlockState());
                        player.level().setBlockAndUpdate(new BlockPos(0,99,0), Blocks.BEDROCK.defaultBlockState());
                    }); next();
                }
                case 13 -> { if (age >= 20) { mc.player.setXRot(90); Neon.autoclicker.setDown(true); next(); } }
                case 14 -> {
                    if (age < 2) return;
                    Neon.autoclicker.setDown(false); mc.gui.setScreen(new PauseScreen(true)); clear(); next();
                }
                case 15 -> {
                    if (age < 35) return;
                    mc.getSingleplayerServer().execute(() -> {
                        boolean air = mc.getSingleplayerServer().overworld().getBlockState(new BlockPos(0,100,0)).isAir();
                        mc.execute(() -> mined = air);
                    }); next();
                }
                case 16 -> {
                    if (age < 6) return;
                    check(mined && Neon.CLICKER.active() && !mc.isPaused(), "held mining breaks a real server block behind Escape");
                    checkPacketOrder("held mining behind Escape");
                    check(ClickerPacketOrder.digging > 0, "observed real mining packets behind Escape");
                    ItemStack tool = mc.player.getMainHandItem();
                    check(tool.isEmpty() || tool.getMaxDamage()-tool.getDamageValue() < 5, "low durability no longer stops automation");
                    Neon.config.leftAction = NeonConfig.LeftAction.CLICK; Neon.config.rightClick = true; clear();
                    mc.disconnect(new TitleScreen(),false); next();
                }
                case 17 -> {
                    if (age < 8) return;
                    check(mc.level == null && Neon.CLICKER.active(), "disconnect retains the user's active toggle");
                    check(LiveSmoke.attacks.isEmpty() && LiveSmoke.uses.isEmpty(), "no world actions are attempted while disconnected");
                    mc.createWorldOpenFlows().openWorld("Neon Verification", () -> {}); next();
                }
                case 18 -> {
                    if (mc.player == null || mc.level == null || mc.gui.screen() != null) return;
                    mc.player.setXRot(-90); clear(); next();
                }
                case 19 -> {
                    if (age < 30) return;
                    check(Neon.CLICKER.active() && interval(LiveSmoke.attacks,2) && interval(LiveSmoke.uses,3),
                        "rejoining resumes the active session without catch-up bursts");
                    checkPacketOrder("gameplay after rejoin");
                    Neon.config.rightClick = false; Neon.config.clickerEntitiesOnly = true;
                    Neon.config.leftIntervalTicks = 1;
                    mc.getSingleplayerServer().execute(() -> {
                        var world = mc.getSingleplayerServer().overworld();
                        world.setBlockAndUpdate(new BlockPos(0,100,0), Blocks.STONE.defaultBlockState());
                        mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().teleportTo(.5,101,.5);
                        ArmorStand stand = new ArmorStand(world,.5,101,3);
                        stand.setInvulnerable(true); stand.setNoGravity(true);
                        world.addFreshEntity(stand); mc.execute(() -> target = stand.getUUID());
                    });
                    mc.player.setYRot(0); mc.player.setXRot(0); clear(); next();
                }
                case 20 -> {
                    if (age < 30) return;
                    check(ClickerPacketOrder.attacks >= 5, "real entity attacks occur during gameplay");
                    checkPacketOrder("entity attacks during gameplay");
                    mc.gui.setScreen(new ChatScreen("Packet-order regression",false)); clear(); next();
                }
                case 21 -> {
                    if (age < 30) return;
                    check(ClickerPacketOrder.attacks >= 5, "real entity attacks continue behind chat");
                    checkPacketOrder("entity attacks behind chat");
                    LiveSmoke.focused = false; mc.gui.setScreen(new PauseScreen(true)); clear(); next();
                }
                case 22 -> {
                    if (age < 30) return;
                    check(ClickerPacketOrder.attacks >= 5, "real entity attacks continue behind an unfocused pause menu");
                    checkPacketOrder("entity attacks behind an unfocused pause menu");
                    LiveSmoke.focused = true; mc.gui.setScreen(null); clear(); next();
                }
                case 23 -> {
                    if (age < 30) return;
                    check(ClickerPacketOrder.attacks >= 5, "real entity attacks resume after closing the menu");
                    checkPacketOrder("entity attacks after closing the menu");
                    Neon.config.clickerEnabled = false;
                    mc.getSingleplayerServer().execute(() -> {
                        var entity = target == null ? null : mc.getSingleplayerServer().overworld().getEntity(target);
                        if (entity != null) entity.discard();
                    });
                    next();
                }
                case 24 -> {
                    if (age < 4) return;
                    check(!Neon.CLICKER.active() && mc.options.pauseOnLostFocus, "manual stop works after rejoin and saved pause settings are unchanged");
                    ClickerPacketOrder.enabled = false;
                    Files.writeString(mc.gameDirectory.toPath().resolve("neon-verification.txt"), "PASS "+checks+" persistent autoclicker checks\n");
                    System.out.println("NEON_PERSISTENT_PASS checks="+checks);
                    done = true; mc.stop();
                }
            }
        } catch (Throwable error) {
            done = true; error.printStackTrace();
            try { Files.writeString(mc.gameDirectory.toPath().resolve("neon-verification.txt"), "FAIL "+error); } catch (Exception ignored) {}
            mc.stop();
        }
    }
    private static final class ProbeScreen extends Screen {
        int clicks;
        EditBox input;
        ProbeScreen() { super(Component.literal("Input isolation")); }
        @Override protected void init() {
            input = addRenderableWidget(new EditBox(font,20,30,200,20,Component.literal("Text")));
            input.setValue("Unchanged text");
            addRenderableWidget(Button.builder(Component.literal("Must not click"), b -> clicks++).bounds(20,60,200,20).build());
        }
    }
}

package me.xchro.neon;

import com.mojang.blaze3d.platform.InputConstants;
import me.xchro.neon.camera.CameraController;
import me.xchro.neon.config.*;
import me.xchro.neon.input.AutoClicker;
import me.xchro.neon.hud.NeonHud;
import me.xchro.neon.render.MotionBlurRenderer;
import me.xchro.neon.ui.SettingsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.*;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lwjgl.glfw.GLFW;

public final class Neon implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("Neon");
    public static NeonConfig config = new NeonConfig();
    public static final CameraController CAMERA = new CameraController();
    public static final AutoClicker CLICKER = new AutoClicker();
    public static final me.xchro.neon.render.TotemPopCounter TOTEM_POPS = new me.xchro.neon.render.TotemPopCounter();
    public static KeyMapping settings, settingsAlternate, freelook, zoom, autoclicker, motionBlur, totemPops, betterChat, betterScreenshot;
    public static final me.xchro.neon.input.ModuleBindings MODULE_BINDINGS = new me.xchro.neon.input.ModuleBindings();
    private static ConfigStore store;
    public static ProfileStore profiles;
    private static final java.util.Queue<String> recoveryNotices = new java.util.ArrayDeque<>();
    public static final QueuedSave SAVES = new QueuedSave((path, failure) -> {
        LOG.error("Could not save {}. Unsaved settings retained in memory.", path, failure);
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.hud.setOverlayMessage(
            net.minecraft.network.chat.Component.literal("Neon: settings could not be saved. Changes retained; check the game log."), false));
    });
    @Override public void onInitializeClient() {
        store = new ConfigStore(FabricLoader.getInstance().getConfigDir().resolve("neon.json"));
        try { config = store.load(); } catch (Exception e) { LOG.error("Could not load Neon settings", e); }
        profiles = new ProfileStore(FabricLoader.getInstance().getConfigDir().resolve("neon-profiles.json"), SAVES);
        try { profiles.initialize(config); } catch (Exception e) { LOG.error("Could not load Neon profiles", e); }
        for(String notice:java.util.List.of(store.recoveryNotice(),profiles.recoveryNotice()))if(!notice.isEmpty()) {
            LOG.warn(notice);recoveryNotices.add(notice);
        }
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("neon", "controls"));
        settings = key("settings", category, GLFW.GLFW_KEY_P);
        settingsAlternate = key("settings_alternate", category, GLFW.GLFW_KEY_RIGHT_SHIFT);
        freelook = key("freelook", category, GLFW.GLFW_KEY_UNKNOWN);
        zoom = key("zoom", category, GLFW.GLFW_KEY_UNKNOWN);
        autoclicker = key("autoclicker", category, GLFW.GLFW_KEY_UNKNOWN);
        motionBlur = key("motion_blur", category, GLFW.GLFW_KEY_UNKNOWN);
        totemPops = key("totem_pops", category, GLFW.GLFW_KEY_UNKNOWN);
        betterChat = key("better_chat", category, GLFW.GLFW_KEY_UNKNOWN);
        betterScreenshot = key("better_screenshot", category, GLFW.GLFW_KEY_UNKNOWN);
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            if(mc.player!=null&&!recoveryNotices.isEmpty())mc.gui.hud.getChat().addClientSystemMessage(
                net.minecraft.network.chat.Component.literal("Neon: "+recoveryNotices.remove()));
            // Drain both queues together so simultaneous shortcuts cannot reopen the menu later.
            while (settings.consumeClick() | settingsAlternate.consumeClick()) {
                if (mc.player != null && mc.gui.screen() == null && mc.gui.overlay() == null)
                    mc.gui.setScreen(new SettingsScreen(null));
            }
            CAMERA.checkContext(mc);
            MODULE_BINDINGS.tick(mc);
            me.xchro.neon.chat.BetterChat.tick(mc);
            CLICKER.tick(mc);
            TOTEM_POPS.tick(mc.level);
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("neon", "equipment"), (graphics, delta) -> NeonHud.draw(graphics, false));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(mc -> {me.xchro.neon.ui.Ui.warmFonts();me.xchro.neon.ui.UiSounds.warm();});
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> mc.execute(() -> {
            save(); SAVES.flush(); CAMERA.reset(mc, true); CLICKER.resetTiming(); MODULE_BINDINGS.reset(); TOTEM_POPS.clear(); MotionBlurRenderer.close();
        }));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
            save(); try { SAVES.close(); } catch (java.util.concurrent.CompletionException failure) { LOG.error("Final settings flush failed", failure); }
            me.xchro.neon.ui.Ui.releaseFonts();me.xchro.neon.ui.UiSounds.close(); MotionBlurRenderer.close();
            me.xchro.neon.screenshot.BetterScreenshot.close();
        });
        LOG.info("Neon {} ready", FabricLoader.getInstance().getModContainer("neon").orElseThrow().getMetadata().getVersion());
    }
    private static KeyMapping key(String name, KeyMapping.Category category, int defaultKey) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neon." + name, InputConstants.Type.KEYSYM, defaultKey, category));
    }
    public static boolean ready(Minecraft mc) {
        return mc.player != null && mc.level != null && mc.player.isAlive() && !mc.player.isSpectator()
            && !mc.player.isSleeping() && mc.getCameraEntity() == mc.player && mc.gui.screen() == null
            && mc.gui.overlay() == null && mc.isWindowActive() && !mc.isPaused();
    }
    public static boolean save() {
        try { if (store != null) store.queue(SAVES, config); return true; }
        catch (Exception e) { LOG.error("Could not save Neon settings", e); return false; }
    }
    public static void flushSaves() { save(); SAVES.flush(); }
    public static boolean applyConfig(NeonConfig settings) {
        Minecraft mc = Minecraft.getInstance(); CAMERA.reset(mc, true);
        MotionBlurRenderer.resetHistory();
        config = ConfigStore.copy(settings);
        MODULE_BINDINGS.reset();
        if (!config.clickerEnabled) CLICKER.cancel(mc);
        else CLICKER.resetTiming();
        return save();
    }
}

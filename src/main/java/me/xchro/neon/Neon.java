package me.xchro.neon;

import com.mojang.blaze3d.platform.InputConstants;
import me.xchro.neon.camera.CameraController;
import me.xchro.neon.config.*;
import me.xchro.neon.input.AutoClicker;
import me.xchro.neon.hud.NeonHud;
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

public final class Neon implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("Neon");
    public static NeonConfig config = new NeonConfig();
    public static final CameraController CAMERA = new CameraController();
    public static final AutoClicker CLICKER = new AutoClicker();
    public static KeyMapping settings, freelook, zoom, autoclicker;
    private static ConfigStore store;
    public static ProfileStore profiles;
    @Override public void onInitializeClient() {
        store = new ConfigStore(FabricLoader.getInstance().getConfigDir().resolve("neon.json"));
        try { config = store.load(); } catch (Exception e) { LOG.error("Could not load Neon settings", e); }
        profiles = new ProfileStore(FabricLoader.getInstance().getConfigDir().resolve("neon-profiles.json"));
        try { profiles.initialize(config); } catch (Exception e) { LOG.error("Could not load Neon profiles", e); }
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("neon", "controls"));
        settings = key("settings", category); freelook = key("freelook", category);
        zoom = key("zoom", category); autoclicker = key("autoclicker", category);
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            while (settings.consumeClick()) if (mc.gui.screen() == null) mc.gui.setScreen(new SettingsScreen(null));
            CAMERA.checkContext(mc);
            CLICKER.tick(mc);
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("neon", "equipment"), (graphics, delta) -> NeonHud.draw(graphics, false));
        LOG.info("Neon {} ready", FabricLoader.getInstance().getModContainer("neon").orElseThrow().getMetadata().getVersion());
    }
    private static KeyMapping key(String name, KeyMapping.Category category) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neon." + name, InputConstants.Type.KEYSYM, -1, category));
    }
    public static boolean ready(Minecraft mc) {
        return mc.player != null && mc.level != null && mc.player.isAlive() && !mc.player.isSpectator()
            && !mc.player.isSleeping() && mc.getCameraEntity() == mc.player && mc.gui.screen() == null
            && mc.gui.overlay() == null && mc.isWindowActive() && !mc.isPaused();
    }
    public static boolean save() {
        try { store.save(config); return true; }
        catch (Exception e) { LOG.error("Could not save Neon settings", e); return false; }
    }
    public static boolean applyConfig(NeonConfig settings) {
        Minecraft mc = Minecraft.getInstance(); CAMERA.reset(mc, true); CLICKER.cancel(mc);
        config = ConfigStore.copy(settings); return save();
    }
}

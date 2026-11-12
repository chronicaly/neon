package me.xchro.neon.module;

import me.xchro.neon.Neon;
import me.xchro.neon.config.NeonConfig;
import me.xchro.neon.core.BindMode;
import net.minecraft.client.KeyMapping;

public enum ClientModule {
    FREELOOK("Freelook", 0), ZOOM("Zoom", 0), MOTION_BLUR("Motion blur", 0),
    TOTEM_POPS("Totem pops", 0), BETTER_CHAT("Better chat", 0), AUTOCLICKER("Autoclicker", 1);
    public final String label;
    public final int page;
    ClientModule(String label, int page) { this.label = label; this.page = page; }
    public static ClientModule named(String label) {
        for (var module : values()) if (module.label.equals(label)) return module;
        return null;
    }
    public KeyMapping key() {
        return switch (this) {
            case FREELOOK -> Neon.freelook; case ZOOM -> Neon.zoom; case MOTION_BLUR -> Neon.motionBlur;
            case TOTEM_POPS -> Neon.totemPops; case AUTOCLICKER -> Neon.autoclicker;
            case BETTER_CHAT -> Neon.betterChat;
        };
    }
    public boolean enabled(NeonConfig c) {
        return switch (this) {
            case FREELOOK -> c.freelookEnabled; case ZOOM -> c.zoomEnabled; case MOTION_BLUR -> c.motionBlurEnabled;
            case TOTEM_POPS -> c.totemPopsEnabled; case AUTOCLICKER -> c.clickerEnabled;
            case BETTER_CHAT -> c.betterChat.enabled;
        };
    }
    public void enabled(NeonConfig c, boolean value) {
        switch (this) {
            case FREELOOK -> c.freelookEnabled = value; case ZOOM -> c.zoomEnabled = value;
            case MOTION_BLUR -> c.motionBlurEnabled = value; case TOTEM_POPS -> c.totemPopsEnabled = value;
            case AUTOCLICKER -> c.clickerEnabled = value;
            case BETTER_CHAT -> c.betterChat.enabled = value;
        }
    }
    public BindMode mode(NeonConfig c) {
        return switch (this) {
            case FREELOOK -> c.freelookMode; case ZOOM -> c.zoomMode(); case MOTION_BLUR -> c.motionBlurMode;
            case TOTEM_POPS -> c.totemPopsMode; case AUTOCLICKER -> c.clickerMode();
            case BETTER_CHAT -> c.betterChat.mode;
        };
    }
    public void mode(NeonConfig c, BindMode value) {
        switch (this) {
            case FREELOOK -> c.freelookMode = value;
            case ZOOM -> { c.zoomMode = value; c.zoomToggle = value == BindMode.TOGGLE; }
            case MOTION_BLUR -> c.motionBlurMode = value;
            case TOTEM_POPS -> c.totemPopsMode = value;
            case AUTOCLICKER -> { c.clickerMode = value; c.toggleClicker = value == BindMode.TOGGLE; }
            case BETTER_CHAT -> c.betterChat.mode = value;
        }
    }
}

package me.xchro.neon.ui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.ConfigStore;
import net.minecraft.client.*;

/** One shared undo history across settings, exact-value dialogs, profiles and the HUD editor. */
public final class SettingsSession {
    private record State(String config, List<String> keys) {}
    private final Deque<State> undo = new ArrayDeque<>();
    private State before;
    private static List<KeyMapping> bindings() { return List.of(Neon.settings, Neon.freelook, Neon.zoom, Neon.autoclicker, Neon.motionBlur, Neon.totemPops, Neon.betterChat, Neon.betterScreenshot); }
    private State capture() { return new State(ConfigStore.encode(Neon.config), bindings().stream().map(KeyMapping::saveString).toList()); }
    public void begin() { if (before == null) before = capture(); }
    public void end() {
        if (before == null) return;
        State old = before; before = null;
        if (!old.equals(capture())) { undo.push(old); while (undo.size() > 50) undo.removeLast(); Neon.save(); }
    }
    public void change(Runnable change) { begin(); try { change.run(); } finally { end(); } }
    public boolean canUndo() { return !undo.isEmpty(); }
    /** Discard only the in-progress gesture, leaving earlier undo entries untouched. */
    public void cancel() {
        if (before == null) return;
        State state = before; before = null;
        if (!state.equals(capture())) restore(state);
    }
    public boolean undo() {
        end(); if (undo.isEmpty()) return false;
        restore(undo.pop()); return true;
    }
    private void restore(State state) {
        boolean keysChanged = false;
        var keys = bindings();
        for (int i = 0; i < keys.size(); i++) if (!keys.get(i).saveString().equals(state.keys().get(i))) {
            keys.get(i).setKey(InputConstants.getKey(state.keys().get(i))); keysChanged = true;
        }
        if (keysChanged) { KeyMapping.resetMapping(); Minecraft.getInstance().options.save(); }
        Neon.applyConfig(ConfigStore.decode(state.config()));
    }
    public void resetPage(int page) {
        change(() -> {
            switch (page) {
                case 0 -> Neon.config.resetCamera();
                case 1 -> Neon.config.resetClicker();
                case 2 -> Neon.config.resetHud();
                case 3 -> {
                    bindings().forEach(key -> key.setKey(key.getDefaultKey()));
                    KeyMapping.resetMapping(); Minecraft.getInstance().options.save();
                }
            }
        });
    }
}

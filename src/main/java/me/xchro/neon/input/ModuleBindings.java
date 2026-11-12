package me.xchro.neon.input;

import java.util.EnumMap;
import me.xchro.neon.Neon;
import me.xchro.neon.core.*;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.Minecraft;

public final class ModuleBindings {
    private static final ClientModule[] PASSIVE = { ClientModule.MOTION_BLUR, ClientModule.TOTEM_POPS, ClientModule.BETTER_CHAT };
    private final EnumMap<ClientModule, VisibilityBinding> states = new EnumMap<>(ClientModule.class);
    private final EnumMap<ClientModule, Boolean> active = new EnumMap<>(ClientModule.class);
    private final EnumMap<ClientModule, String> keys = new EnumMap<>(ClientModule.class);
    public void tick(Minecraft mc) {
        boolean interactive = Neon.ready(mc);
        for (var module : PASSIVE) {
            var key = module.key(); if (key == null) continue;
            var state = states.computeIfAbsent(module, ignored -> new VisibilityBinding());
            if (!key.saveString().equals(keys.put(module, key.saveString()))) state.reset();
            var next = state.update(module.enabled(Neon.config), interactive, !key.isUnbound(), key.isDown(), module.mode(Neon.config));
            if (next.enabled() != module.enabled(Neon.config)) { module.enabled(Neon.config, next.enabled()); Neon.save(); }
            active.put(module, next.active());
        }
    }
    public boolean active(ClientModule module) {
        if (!module.enabled(Neon.config)) return false;
        var key = module.key();
        return key == null || key.isUnbound() || module.mode(Neon.config) == BindMode.TOGGLE || active.getOrDefault(module, false);
    }
    public void reset() { states.values().forEach(VisibilityBinding::reset); active.clear(); keys.clear(); }
}

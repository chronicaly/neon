package me.xchro.neon.render;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.xchro.neon.Neon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.player.Player;

/** Client-thread, world-local counts of server-confirmed totem activations. */
public final class TotemPopCounter {
    private final Map<UUID, Integer> counts = new HashMap<>();
    private ClientLevel world;

    private void world(ClientLevel level) {
        if (world != level) { counts.clear(); world = level; }
    }

    public void tick(ClientLevel level) {
        world(level);
        if (level != null && !counts.isEmpty()) {
            for (Player player : level.players()) if (!player.isAlive()) forget(player.getUUID());
        }
    }

    public void event(ClientLevel level, Entity entity, byte event) {
        world(level);
        if (level == null || !(entity instanceof Player player)) return;
        if (event == EntityEvent.PROTECTED_FROM_DEATH) {
            counts.merge(player.getUUID(), 1, (count, one) -> count == Integer.MAX_VALUE ? count : count + one);
        } else if (event == EntityEvent.DEATH) forget(player.getUUID());
    }

    public int count(UUID player) { return counts.getOrDefault(player, 0); }
    public void forget(UUID player) { counts.remove(player); }
    public void clear() { counts.clear(); world = null; }

    public Component nameTag(Entity entity, Component name) {
        if (!Neon.MODULE_BINDINGS.active(me.xchro.neon.module.ClientModule.TOTEM_POPS) || !(entity instanceof Player) || name == null
            || entity.level() != world || !entity.isAlive()) return name;
        int count = count(entity.getUUID());
        if (count == 0) return name;
        // A neutral parent preserves the original team formatting without inheriting it on the counter.
        return Component.empty().append(name).append(Component.literal(" -" + count)
            .withColor(color(count, Neon.config.totemPopsRedAt)));
    }

    public static int color(int count, int redAt) {
        double progress = Math.clamp((count - 1.0) / (Math.max(2, redAt) - 1.0), 0, 1);
        // Vanilla green -> yellow -> red; clamp the color, never the displayed count.
        int red = 0x55 + (int) Math.round(0xaa * Math.min(1, progress * 2));
        int green = 0xff - (int) Math.round(0xaa * Math.max(0, progress * 2 - 1));
        return red << 16 | green << 8 | 0x55;
    }
}

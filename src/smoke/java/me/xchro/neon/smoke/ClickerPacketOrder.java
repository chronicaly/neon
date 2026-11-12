package me.xchro.neon.smoke;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;

/** Observes actual outgoing packets in the disposable client, without changing them. */
public final class ClickerPacketOrder {
    public static boolean enabled;
    public static int boundaries, actions, attacks, uses, digging;
    public static String failure;
    private static boolean afterMovement;

    private ClickerPacketOrder() {}

    public static void beginTick() { afterMovement = false; }

    public static void clear() {
        boundaries = actions = attacks = uses = digging = 0;
        failure = null;
    }

    public static void sent(Packet<?> packet) {
        if (!enabled) return;
        if (packet instanceof ServerboundMovePlayerPacket || packet instanceof ServerboundClientTickEndPacket) {
            afterMovement = true;
            boundaries++;
            return;
        }
        boolean action = packet instanceof ServerboundAttackPacket || packet instanceof ServerboundSwingPacket
            || packet instanceof ServerboundUseItemPacket || packet instanceof ServerboundUseItemOnPacket
            || packet instanceof ServerboundInteractPacket || packet instanceof ServerboundPlayerActionPacket;
        if (!action) return;
        actions++;
        if (packet instanceof ServerboundAttackPacket) attacks++;
        if (packet instanceof ServerboundUseItemPacket || packet instanceof ServerboundUseItemOnPacket) uses++;
        if (packet instanceof ServerboundPlayerActionPacket) digging++;
        if (afterMovement && failure == null)
            failure = packet.getClass().getSimpleName() + " after movement/tick-end at tick " + LiveSmoke.currentTick;
    }
}

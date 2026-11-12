package me.xchro.neon.mixin;

import me.xchro.neon.Neon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class TotemPopPacketMixin {
    @Shadow private ClientLevel level;

    // RETURN runs only after vanilla's packet-thread handoff, so each event counts once.
    @Inject(method = "handleEntityEvent", at = @At("RETURN"))
    private void neon$totemPop(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (level != null) Neon.TOTEM_POPS.event(level, packet.getEntity(level), packet.getEventId());
    }

    @Inject(method = "handlePlayerInfoRemove", at = @At("RETURN"))
    private void neon$playerLeft(ClientboundPlayerInfoRemovePacket packet, CallbackInfo ci) {
        packet.profileIds().forEach(Neon.TOTEM_POPS::forget);
    }
}

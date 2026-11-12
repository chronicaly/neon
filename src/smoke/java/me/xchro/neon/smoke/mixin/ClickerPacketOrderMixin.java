package me.xchro.neon.smoke.mixin;

import me.xchro.neon.smoke.ClickerPacketOrder;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
abstract class ClickerPacketOrderMixin {
    @Inject(method = "send", at = @At("HEAD"))
    private void packet(Packet<?> packet, CallbackInfo ci) { ClickerPacketOrder.sent(packet); }
}

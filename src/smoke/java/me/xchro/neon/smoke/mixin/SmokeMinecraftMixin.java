package me.xchro.neon.smoke.mixin;

import me.xchro.neon.smoke.LiveSmoke;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Deterministic focus simulation in the isolated verification client only. */
@Mixin(Minecraft.class)
public abstract class SmokeMinecraftMixin {
    @Inject(method = "isWindowActive", at = @At("HEAD"), cancellable = true)
    private void focus(CallbackInfoReturnable<Boolean> cir) { cir.setReturnValue(LiveSmoke.focused); }
}

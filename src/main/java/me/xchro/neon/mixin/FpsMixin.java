package me.xchro.neon.mixin;

import me.xchro.neon.hud.FpsHud;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class FpsMixin {
    @Inject(method = "renderFrame", at = @At("HEAD"))
    private void neon$fps(boolean advanceGameTime, CallbackInfo ci) {
        if (advanceGameTime) FpsHud.frame(System.nanoTime());
    }
}

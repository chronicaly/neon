package me.xchro.neon.mixin;

import me.xchro.neon.render.MotionBlurRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MotionBlurMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
    private void neon$blurWorld(DeltaTracker tracker, boolean advanceGameTime, CallbackInfo ci) { MotionBlurRenderer.render(true); }

    @Inject(method = "render", at = @At("RETURN"))
    private void neon$blurFrame(DeltaTracker tracker, boolean advanceGameTime, CallbackInfo ci) { MotionBlurRenderer.render(false); }

    @Inject(method = "close", at = @At("HEAD"))
    private void neon$closeBlur(CallbackInfo ci) { MotionBlurRenderer.close(); }
}

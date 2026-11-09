package me.xchro.neon.smoke.mixin;

import me.xchro.neon.render.MotionBlurRenderer;
import me.xchro.neon.smoke.MotionBlurSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MotionBlurRenderer.class, remap = false)
public class SmokeMotionBlurMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private static void before(boolean beforeUi, CallbackInfo ci) {
        if (MotionBlurSmoke.active != null) MotionBlurSmoke.active.before(beforeUi);
    }
    @Inject(method = "render", at = @At("RETURN"))
    private static void after(boolean beforeUi, CallbackInfo ci) {
        if (MotionBlurSmoke.active != null) MotionBlurSmoke.active.after(beforeUi);
    }
}

package me.xchro.neon.smoke.mixin;
import me.xchro.neon.smoke.PerformanceSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets = "me.xchro.neon.render.MotionBlurRenderer", remap = false)
public abstract class PerfBlurMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private static void start0(CallbackInfo ci) { PerformanceSmoke.begin(); }
    @Inject(method = "render", at = @At("RETURN"))
    private static void end0(CallbackInfo ci) { PerformanceSmoke.end(); }
}

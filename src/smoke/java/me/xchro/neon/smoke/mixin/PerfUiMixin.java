package me.xchro.neon.smoke.mixin;
import me.xchro.neon.smoke.PerformanceSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets = "me.xchro.neon.ui.PixelScreen", remap = false)
public abstract class PerfUiMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void start0(CallbackInfo ci) { PerformanceSmoke.begin(); }
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void end0(CallbackInfo ci) { PerformanceSmoke.end(); }
    @Inject(method = "extractBackground", at = @At("HEAD"))
    private void start1(CallbackInfo ci) { PerformanceSmoke.begin(); }
    @Inject(method = "extractBackground", at = @At("RETURN"))
    private void end1(CallbackInfo ci) { PerformanceSmoke.end(); }
}

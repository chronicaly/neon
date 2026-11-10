package me.xchro.neon.smoke.mixin;
import me.xchro.neon.smoke.PerformanceSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets = "me.xchro.neon.ui.UiEffects$Border", remap = false)
public abstract class PerfBorderMixin {
    @Inject(method = "buildVertices", at = @At("HEAD"))
    private void start0(CallbackInfo ci) { PerformanceSmoke.begin(); }
    @Inject(method = "buildVertices", at = @At("RETURN"))
    private void end0(CallbackInfo ci) { PerformanceSmoke.end(); }
}

package me.xchro.neon.smoke.mixin;
import me.xchro.neon.smoke.PerformanceSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets = "me.xchro.neon.ui.UiEffects$Border", remap = false)
public abstract class PerfBorderMixin {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private int x,y,w,h;
    @Inject(method = "buildVertices", at = @At("HEAD"))
    private void start0(CallbackInfo ci) { PerformanceSmoke.begin(); me.xchro.neon.smoke.ModuleGridSmoke.border(x,y,w,h); }
    @Inject(method = "buildVertices", at = @At("RETURN"))
    private void end0(CallbackInfo ci) { PerformanceSmoke.end(); }
}

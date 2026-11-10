package me.xchro.neon.smoke.mixin;
import me.xchro.neon.smoke.PerformanceSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets = "me.xchro.neon.hud.NeonHud", remap = false)
public abstract class PerfHudMixin {
    @Inject(method = "draw", at = @At("HEAD"))
    private static void start0(CallbackInfo ci) { PerformanceSmoke.begin(); }
    @Inject(method = "draw", at = @At("RETURN"))
    private static void end0(CallbackInfo ci) { PerformanceSmoke.end(); }
    @Inject(method = "offhandCount", at = @At("HEAD"))
    private static void start1(CallbackInfoReturnable<String> ci) { PerformanceSmoke.begin(); }
    @Inject(method = "offhandCount", at = @At("RETURN"))
    private static void end1(CallbackInfoReturnable<String> ci) { PerformanceSmoke.end(); }
}

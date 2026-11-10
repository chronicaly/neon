package me.xchro.neon.smoke.mixin;
import me.xchro.neon.smoke.PerformanceSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets = "me.xchro.neon.camera.CameraController", remap = false)
public abstract class PerfCameraMixin {
    @Inject(method = {"frame","checkContext"}, at = @At("HEAD"))
    private void start0(CallbackInfo ci) { PerformanceSmoke.begin(); }
    @Inject(method = {"frame","checkContext"}, at = @At("RETURN"))
    private void end0(CallbackInfo ci) { PerformanceSmoke.end(); }
}

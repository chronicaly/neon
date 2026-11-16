package me.xchro.neon.smoke.mixin;
import me.xchro.neon.smoke.PerformanceSmoke;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Includes deferred surface work in both the 0.5.1 baseline and the rounded renderer. */
@Pseudo
@Mixin(targets={"me.xchro.neon.ui.RoundedUi$Stroke","me.xchro.neon.ui.RoundedUi$Fill","me.xchro.neon.hud.HudGlassRenderer$Glass"},remap=false)
public abstract class PerfSurfaceMixin {
    @Inject(method="buildVertices",at=@At("HEAD")) private void begin(CallbackInfo ci){PerformanceSmoke.begin();}
    @Inject(method="buildVertices",at=@At("RETURN")) private void end(CallbackInfo ci){PerformanceSmoke.end();}
}

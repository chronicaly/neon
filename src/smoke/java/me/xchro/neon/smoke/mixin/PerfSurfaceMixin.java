package me.xchro.neon.smoke.mixin;
import me.xchro.neon.smoke.PerformanceSmoke;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Includes deferred Neon surface and batched chat-glyph work; absent baseline classes are skipped. */
@Pseudo
@Mixin(targets={"me.xchro.neon.ui.RoundedUi$Stroke","me.xchro.neon.ui.RoundedUi$Fill","me.xchro.neon.hud.HudGlassRenderer$Glass","me.xchro.neon.ui.UiFont$GlyphBatchState"},remap=false)
public abstract class PerfSurfaceMixin {
    @Inject(method="buildVertices",at=@At("HEAD")) private void begin(CallbackInfo ci){PerformanceSmoke.begin();}
    @Inject(method="buildVertices",at=@At("RETURN")) private void end(CallbackInfo ci){PerformanceSmoke.end();}
}

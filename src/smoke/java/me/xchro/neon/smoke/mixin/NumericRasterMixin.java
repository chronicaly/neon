package me.xchro.neon.smoke.mixin;

import me.xchro.neon.smoke.NumericTextSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Text instrumentation is confined to the verification mod. */
@Mixin(targets="me.xchro.neon.ui.TextRaster",remap=false)
public abstract class NumericRasterMixin {
    @Inject(method="render",at=@At("HEAD"))
    private static void start(CallbackInfoReturnable<?> result) { NumericTextSmoke.rasterStart(); }
    @Inject(method="render",at=@At("RETURN"))
    private static void end(CallbackInfoReturnable<?> result) { NumericTextSmoke.rasterEnd(); }
}

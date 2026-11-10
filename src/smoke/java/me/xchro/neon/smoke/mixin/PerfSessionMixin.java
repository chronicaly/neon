package me.xchro.neon.smoke.mixin;
import me.xchro.neon.smoke.PerformanceSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets="me.xchro.neon.ui.SettingsSession",remap=false)
abstract class PerfSessionMixin {
    @Inject(method={"begin","end","change"},at=@At("HEAD"))
    private void start(CallbackInfo ci){PerformanceSmoke.begin();}
    @Inject(method={"begin","end","change"},at=@At("RETURN"))
    private void end(CallbackInfo ci){PerformanceSmoke.end();}
}

package me.xchro.neon.smoke.mixin;
import me.xchro.neon.smoke.PerformanceSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets="me.xchro.neon.input.AutoClicker",remap=false)
abstract class PerfClickerMixin {
    @Inject(method="tick",at=@At("HEAD"))
    private void start(CallbackInfo ci){PerformanceSmoke.begin();}
    @Inject(method="tick",at=@At("RETURN"))
    private void end(CallbackInfo ci){PerformanceSmoke.end();}
}

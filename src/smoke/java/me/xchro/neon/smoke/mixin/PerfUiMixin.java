package me.xchro.neon.smoke.mixin;
import me.xchro.neon.smoke.PerformanceSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets = "me.xchro.neon.ui.PixelScreen", remap = false)
public abstract class PerfUiMixin {
    @ModifyArgs(method="extractRenderState",at=@At(value="INVOKE",target="Lme/xchro/neon/ui/PixelScreen;renderPixels(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void hoverPointer(org.spongepowered.asm.mixin.injection.invoke.arg.Args args) {
        var point=me.xchro.neon.smoke.PremiumSmoke.pointer();
        if(point==null)point=me.xchro.neon.smoke.HoverGlowSmoke.pointer();
        if(point==null)point=me.xchro.neon.smoke.ModuleGridSmoke.pointer();
        if(point!=null){args.set(1,point[0]);args.set(2,point[1]);}
    }
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void start0(CallbackInfo ci) { PerformanceSmoke.begin(); me.xchro.neon.smoke.NumericTextSmoke.uiStart(); }
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void end0(CallbackInfo ci) { PerformanceSmoke.end(); me.xchro.neon.smoke.NumericTextSmoke.uiEnd(); }
    @Inject(method = "extractBackground", at = @At("HEAD"))
    private void start1(CallbackInfo ci) { PerformanceSmoke.begin(); }
    @Inject(method = "extractBackground", at = @At("RETURN"))
    private void end1(CallbackInfo ci) { PerformanceSmoke.end(); }
}

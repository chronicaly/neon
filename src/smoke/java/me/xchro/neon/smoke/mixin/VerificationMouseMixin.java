package me.xchro.neon.smoke.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The disposable test client receives scripted screen input, never desktop pointer input. */
@Mixin(MouseHandler.class)
abstract class VerificationMouseMixin {
    @Inject(method={"onButton","onMove","onScroll"},at=@At("HEAD"),cancellable=true)
    private void neon$isolateDesktop(CallbackInfo ci) { ci.cancel(); }
}

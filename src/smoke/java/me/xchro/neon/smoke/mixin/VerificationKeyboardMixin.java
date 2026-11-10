package me.xchro.neon.smoke.mixin;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
abstract class VerificationKeyboardMixin {
    @Inject(method={"keyPress","charTyped"},at=@At("HEAD"),cancellable=true)
    private void neon$isolateDesktop(CallbackInfo ci) { ci.cancel(); }
}

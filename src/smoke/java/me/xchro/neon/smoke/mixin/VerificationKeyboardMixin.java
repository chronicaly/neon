package me.xchro.neon.smoke.mixin;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyboardHandler.class)
abstract class VerificationKeyboardMixin {
    @Unique private String neon$clipboard="";
    @Inject(method="getClipboard",at=@At("HEAD"),cancellable=true)
    private void neon$readTestClipboard(CallbackInfoReturnable<String> ci) { ci.setReturnValue(neon$clipboard); }
    @Inject(method="setClipboard",at=@At("HEAD"),cancellable=true)
    private void neon$writeTestClipboard(String text,CallbackInfo ci) { neon$clipboard=text;ci.cancel(); }
    @Inject(method={"keyPress","charTyped"},at=@At("HEAD"),cancellable=true)
    private void neon$isolateDesktop(CallbackInfo ci) { ci.cancel(); }
}

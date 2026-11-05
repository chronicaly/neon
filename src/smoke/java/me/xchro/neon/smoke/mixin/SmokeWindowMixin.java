package me.xchro.neon.smoke.mixin;

import com.mojang.blaze3d.platform.Window;
import me.xchro.neon.smoke.LiveSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public abstract class SmokeWindowMixin {
    @Inject(method = "isFocused", at = @At("HEAD"), cancellable = true)
    private void focus(CallbackInfoReturnable<Boolean> cir) { cir.setReturnValue(LiveSmoke.focused); }
}

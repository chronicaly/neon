package me.xchro.neon.smoke.mixin;

import me.xchro.neon.Neon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The isolated clicker scenario supplies deterministic key states, including behind menus. */
@Mixin(targets="me.xchro.neon.input.AutoClicker",remap=false)
abstract class ClickerActivationInputMixin {
    @Inject(method="activationDown",at=@At("HEAD"),cancellable=true)
    private void controlledKey(CallbackInfoReturnable<Boolean> result) {
        if(Boolean.getBoolean("neon.smoke.clickerOnly"))result.setReturnValue(Neon.autoclicker.isDown());
    }
}

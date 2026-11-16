package me.xchro.neon.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.function.BooleanSupplier;
import me.xchro.neon.Neon;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Use the game's own toggle semantics, including release and screen transitions. */
@Mixin(ToggleKeyMapping.class)
public abstract class ToggleKeyMixin {
    private boolean neon$sprint() { return (Object)this==Minecraft.getInstance().options.keySprint&&ClientModule.TOGGLE_SPRINT.active(); }
    private boolean neon$sneak() { return (Object)this==Minecraft.getInstance().options.keyShift&&ClientModule.TOGGLE_SNEAK.active(); }
    @WrapOperation(method={"setDown","shouldSetOnIngameFocus","release","shouldRestoreStateOnScreenClosed"},at=@At(value="INVOKE",target="Ljava/util/function/BooleanSupplier;getAsBoolean()Z"))
    private boolean neon$toggle(BooleanSupplier supplier,Operation<Boolean> original) { return original.call(supplier)||neon$sprint()||neon$sneak(); }
    @ModifyReturnValue(method="shouldRestoreStateOnScreenClosed",at=@At("RETURN"))
    private boolean neon$resume(boolean original) { return original&&(!neon$sprint()||Neon.config.sprintResume)&&(!neon$sneak()||Neon.config.sneakResume); }
}

package me.xchro.neon.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.xchro.neon.Neon;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractClientPlayer.class)
public abstract class FovEffectsMixin {
    @ModifyReturnValue(method="getFieldOfViewModifier",at=@At("RETURN"))
    private float neon$effectStrength(float original) {
        // Scale only vanilla movement/use effects. The chosen base FOV and Neon's zoom remain separate.
        return ClientModule.DYNAMIC_FOV.active()?1+(original-1)*(float)Neon.config.fovEffectStrength:original;
    }
}

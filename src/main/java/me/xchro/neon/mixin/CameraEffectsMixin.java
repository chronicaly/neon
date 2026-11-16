package me.xchro.neon.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.xchro.neon.Neon;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public abstract class CameraEffectsMixin {
    @ModifyExpressionValue(method="bobView",at=@At(value="FIELD",target="Lnet/minecraft/client/renderer/state/level/CameraEntityRenderState;bob:F"))
    private float neon$walkingBob(float original) {
        return ClientModule.VIEW_BOBBING.active()?original*(float)Neon.config.viewBobbingStrength:original;
    }
    @ModifyExpressionValue(method="bobHurt",at=@At(value="FIELD",target="Lnet/minecraft/client/renderer/state/OptionsRenderState;damageTiltStrength:D"))
    private double neon$damageTilt(double original) {
        // Leave hit direction, death camera, damage and invulnerability state untouched.
        return ClientModule.HURT_CAMERA.active()?original*Neon.config.hurtCameraStrength:original;
    }
}

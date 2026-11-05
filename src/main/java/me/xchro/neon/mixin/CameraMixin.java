package me.xchro.neon.mixin;

import me.xchro.neon.Neon;
import net.minecraft.client.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "update", at = @At("HEAD"))
    private void neon$frame(DeltaTracker delta, CallbackInfo ci) { Neon.CAMERA.frame(Minecraft.getInstance()); }
    @ModifyExpressionValue(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float neon$yaw(float original) { return Neon.CAMERA.yaw(original); }
    @ModifyExpressionValue(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float neon$pitch(float original) { return Neon.CAMERA.pitch(original); }
    @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"), index = 0)
    private float neon$distance(float distance) { return Neon.CAMERA.ownsCamera() ? distance * (float) Neon.CAMERA.distanceFactor() : distance; }
    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float neon$zoom(float original) { return Neon.CAMERA.fov(original); }
}

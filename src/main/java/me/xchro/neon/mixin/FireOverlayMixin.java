package me.xchro.neon.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import me.xchro.neon.Neon;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class FireOverlayMixin {
    // Unity MixinNoRenderEffects: transform the actual quad, after deferred node submission.
    // Minecraft supplies a disposable matrix for each side; the caller's pose remains untouched.
    @Inject(method="buildFireQuad",at=@At("HEAD"))
    private static void neon$lowerFire(TextureAtlasSprite sprite,VertexConsumer vertices,Matrix4f pose,CallbackInfo ci) {
        if(ClientModule.LOW_FIRE.active())pose.translate(0,-(float)Neon.config.fireLowering*.7f,0);
    }
}

package me.xchro.neon.mixin;

import me.xchro.neon.Neon;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class FullbrightMixin {
    @Inject(method="extract",at=@At("RETURN"))
    private void neon$brightness(LightmapRenderState state,float partial,CallbackInfo ci) {
        if(!state.needsUpdate||!ClientModule.FULLBRIGHT.active())return;
        var player=Minecraft.getInstance().player;
        if(player==null||player.hasEffect(MobEffects.DARKNESS)||player.hasEffect(MobEffects.BLINDNESS))return;
        // Reuse the shader's luminance normalization without granting a status effect or editing gamma.
        state.nightVisionEffectIntensity=Math.max(state.nightVisionEffectIntensity,(float)Neon.config.fullbrightStrength);
    }
}

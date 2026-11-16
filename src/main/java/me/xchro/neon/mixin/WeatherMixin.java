package me.xchro.neon.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.xchro.neon.Neon;
import me.xchro.neon.config.NeonConfig;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.renderer.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Change precipitation and sky visibility only while extracting their render states. */
@Mixin({WeatherEffectRenderer.class,SkyRenderer.class})
public abstract class WeatherMixin {
    @ModifyExpressionValue(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
    private float neon$precipitation(float original) {
        if(!ClientModule.WEATHER.active())return original;
        return Neon.config.weather==NeonConfig.Weather.CLEAR?0:(float)Neon.config.weatherIntensity;
    }
}

package me.xchro.neon.mixin;

import me.xchro.neon.Neon;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class RenderOptionsMixin {
    @Shadow @Final private GameRenderState gameRenderState;
    @Inject(method="extractOptions",at=@At("TAIL"))
    private void neon$glint(CallbackInfo ci) {
        if(!ClientModule.ENCHANT_GLINT.active())return;
        var options=gameRenderState.optionsRenderState;
        options.glintStrength=Neon.config.glintStrength;options.glintSpeed=Neon.config.glintSpeed;
    }
}

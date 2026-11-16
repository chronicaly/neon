package me.xchro.neon.mixin;

import me.xchro.neon.Neon;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.gui.*;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class CleanViewMixin {
    private static final Identifier NEON_PUMPKIN=Identifier.withDefaultNamespace("textures/misc/pumpkinblur.png");
    @Inject(method="extractTextureOverlay",at=@At("HEAD"),cancellable=true)
    private void neon$pumpkin(GuiGraphicsExtractor g,Identifier texture,float alpha,CallbackInfo ci) {
        if(ClientModule.CLEAN_VIEW.active()&&Neon.config.hidePumpkin&&texture.equals(NEON_PUMPKIN))ci.cancel();
    }
    @Inject(method="extractPortalOverlay",at=@At("HEAD"),cancellable=true)
    private void neon$portal(CallbackInfo ci) { if(ClientModule.CLEAN_VIEW.active()&&Neon.config.hidePortal)ci.cancel(); }
    @Inject(method="extractVignette",at=@At("HEAD"),cancellable=true)
    private void neon$vignette(CallbackInfo ci) { if(ClientModule.CLEAN_VIEW.active()&&Neon.config.hideVignette)ci.cancel(); }
}

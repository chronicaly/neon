package me.xchro.neon.mixin;

import me.xchro.neon.Neon;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.renderer.texture.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Recolor the existing damage rows once per color change; preserve white flashes and texture ownership. */
@Mixin(OverlayTexture.class)
public abstract class HitColorMixin {
    @Shadow @Final private DynamicTexture texture;
    @Unique private int[] neon$original;
    @Unique private boolean neon$custom;
    @Unique private int neon$lastColor;
    @Inject(method="<init>",at=@At("TAIL"))
    private void neon$remember(CallbackInfo ci) {
        var pixels=texture.getPixels();neon$original=new int[128];
        for(int y=0;y<8;y++)for(int x=0;x<16;x++)neon$original[y*16+x]=pixels.getPixel(x,y);
    }
    @Inject(method="getTextureView",at=@At("HEAD"))
    private void neon$tint(CallbackInfoReturnable<com.mojang.blaze3d.textures.GpuTextureView> cir) {
        boolean custom=ClientModule.HIT_COLOR.active();
        if(!custom&&!neon$custom)return;
        int color=Neon.config.hitColor();
        // The overlay shader stores transmission alpha, the inverse of the color picker's opacity.
        int packed=((255-(int)Math.round((color>>>24)*Neon.config.hitColorStrength))<<24)|(color&0xffffff);
        if(custom&&neon$custom&&packed==neon$lastColor)return;
        var pixels=texture.getPixels();if(pixels==null||neon$original==null)return;
        for(int y=0;y<8;y++)for(int x=0;x<16;x++)pixels.setPixel(x,y,custom?packed:neon$original[y*16+x]);
        texture.upload();neon$custom=custom;neon$lastColor=packed;
    }
}

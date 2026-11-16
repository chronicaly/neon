package me.xchro.neon.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.xchro.neon.hud.NeonHud;
import net.minecraft.client.gui.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
    @Inject(method="extractScoreboardSidebar",at=@At("HEAD"),cancellable=true)
    private void neon$scoreboard(CallbackInfo ci) {
        if(me.xchro.neon.Neon.config.hud(me.xchro.neon.hud.HudKind.SCOREBOARD).placement.visible)ci.cancel();
    }
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private static net.minecraft.resources.Identifier CROSSHAIR_SPRITE;
    @WrapOperation(method="extractCrosshair",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void neon$reticle(GuiGraphicsExtractor g,com.mojang.blaze3d.pipeline.RenderPipeline pipeline,net.minecraft.resources.Identifier sprite,int x,int y,int w,int h,Operation<Void> original) {
        if(sprite.equals(CROSSHAIR_SPRITE)&&me.xchro.neon.module.ClientModule.CROSSHAIR.active())me.xchro.neon.render.CrosshairRenderer.draw(g);
        else original.call(g,pipeline,sprite,x,y,w,h);
    }
    @Inject(method = "extractChat", at = @At("HEAD"), cancellable = true)
    private void neon$editorChat(CallbackInfo ci) {
        if (net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof me.xchro.neon.ui.HudEditor) ci.cancel();
    }
    @WrapOperation(method = "extractSlot", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"))
    private void neon$offhandTotal(GuiGraphicsExtractor g, Font font, ItemStack stack, int x, int y, Operation<Void> original) {
        String count = NeonHud.offhandCount(stack);
        if (count == null) original.call(g, font, stack, x, y);
        else g.itemDecorations(font, stack, x, y, count);
    }
}

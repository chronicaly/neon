package me.xchro.neon.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.xchro.neon.ui.PixelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native hover clipping expects GUI coordinates, while Neon widgets receive canvas pixels. */
@Mixin(AbstractWidget.class)
public abstract class UiWidgetMixin {
    @Inject(method="playDownSound",at=@At("HEAD"),cancellable=true)
    private void neon$customSound(net.minecraft.client.sounds.SoundManager sounds,CallbackInfo ci) {
        if(Minecraft.getInstance().gui.screen() instanceof PixelScreen)ci.cancel();
    }
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;containsPointInScissor(II)Z"))
    private boolean neon$pixelHover(GuiGraphicsExtractor g, int x, int y, Operation<Boolean> original) {
        if (Minecraft.getInstance().gui.screen() instanceof PixelScreen) {
            Vector2f point = g.pose().transformPosition(x, y, new Vector2f());
            return original.call(g, (int) Math.floor(point.x), (int) Math.floor(point.y));
        }
        return original.call(g, x, y);
    }
}

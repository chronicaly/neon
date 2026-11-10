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

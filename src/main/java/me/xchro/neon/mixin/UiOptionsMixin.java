package me.xchro.neon.mixin;

import me.xchro.neon.ui.HudEditor;
import me.xchro.neon.ui.PixelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Neon's backdrop has its own strength without changing the user's Minecraft preference. */
@Mixin(Options.class)
public abstract class UiOptionsMixin {
    @Inject(method = "getMenuBackgroundBlurriness", at = @At("HEAD"), cancellable = true)
    private void neon$backdropStrength(CallbackInfoReturnable<Integer> result) {
        var screen = Minecraft.getInstance().gui.screen();
        int exitBlur=me.xchro.neon.ui.MenuPresentation.exitBlur();
        if(exitBlur>=0){result.setReturnValue(exitBlur);return;}
        if (screen instanceof PixelScreen && !(screen instanceof HudEditor)) result.setReturnValue(5);
    }
}

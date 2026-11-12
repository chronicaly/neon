package me.xchro.neon.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.function.Consumer;
import me.xchro.neon.screenshot.BetterScreenshot;
import net.minecraft.client.Screenshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Unity's callback wrapper leaves vanilla's PNG save and clickable result intact. */
@Mixin(Screenshot.class)
abstract class ScreenshotMixin {
    @ModifyArg(method = "grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Screenshot;takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V"), index = 2)
    private static Consumer<NativeImage> neon$copy(Consumer<NativeImage> original) {
        return image -> {
            try { BetterScreenshot.capture(image); }
            finally { original.accept(image); }
        };
    }
}

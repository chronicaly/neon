package me.xchro.neon.smoke.mixin;

import me.xchro.neon.smoke.ClipboardAudit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No verification screenshot may replace the user's real system clipboard. */
@org.spongepowered.asm.mixin.Pseudo
@Mixin(targets = "me.xchro.neon.screenshot.WindowsImageClipboard", remap = false)
abstract class ScreenshotClipboardMixin {
    @Inject(method = "copy", at = @At("HEAD"), cancellable = true)
    private static void neon$clipboard(long window, int width, int height, int[] pixels, CallbackInfo ci) {
        ClipboardAudit.copy(width, height, pixels); ci.cancel();
    }
}

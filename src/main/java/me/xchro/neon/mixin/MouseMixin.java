package me.xchro.neon.mixin;

import me.xchro.neon.Neon;
import net.minecraft.client.*;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.*;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {
    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void neon$turn(LocalPlayer player, double dx, double dy, Operation<Void> original) {
        Neon.CAMERA.frame(Minecraft.getInstance());
        dx = Neon.CAMERA.mouse(dx); dy = Neon.CAMERA.mouse(dy);
        if (Neon.CAMERA.looking()) Neon.CAMERA.turn(dx, dy);
        else original.call(player, dx, dy);
    }
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void neon$scroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (window == Minecraft.getInstance().getWindow().handle() && Neon.CAMERA.scroll(vertical)) ci.cancel();
    }
}

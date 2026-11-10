package me.xchro.neon.smoke.mixin;

import me.xchro.neon.smoke.LiveSmoke;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Deterministic focus simulation in the isolated verification client only. */
@Mixin(Minecraft.class)
public abstract class SmokeMinecraftMixin {
    @Inject(method = "renderFrame", at = @At("HEAD"))
    private void frameStart(CallbackInfo ci) { me.xchro.neon.smoke.PerformanceSmoke.frameStart(); }
    @Inject(method = "renderFrame", at = @At("RETURN"))
    private void frameEnd(CallbackInfo ci) { me.xchro.neon.smoke.PerformanceSmoke.frameEnd(); }
    @Shadow private int missTime;
    @Shadow private int rightClickDelay;
    @Inject(method = "tick", at = @At("HEAD"))
    private void clock(CallbackInfo ci) { LiveSmoke.currentTick++; }
    @Inject(method = "isWindowActive", at = @At("HEAD"), cancellable = true)
    private void focus(CallbackInfoReturnable<Boolean> cir) { cir.setReturnValue(LiveSmoke.focused); }
    @Inject(method = "startAttack", at = @At("HEAD"))
    private void attack(CallbackInfoReturnable<Boolean> cir) {
        if (LiveSmoke.trackClicks && missTime <= 0) {
            LiveSmoke.attacks.add(LiveSmoke.currentTick);
            LiveSmoke.strengths.add(Minecraft.getInstance().player.getAttackStrengthScale(0));
        }
    }
    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void use(CallbackInfo ci) {
        if (LiveSmoke.trackClicks) { LiveSmoke.uses.add(LiveSmoke.currentTick); LiveSmoke.useDelays.add(rightClickDelay); }
    }
}

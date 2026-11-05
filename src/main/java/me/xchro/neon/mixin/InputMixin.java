package me.xchro.neon.mixin;

import me.xchro.neon.Neon;
import net.minecraft.client.*;
import org.spongepowered.asm.mixin.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class InputMixin {
    @Shadow private int missTime;
    @Shadow private int rightClickDelay;
    @Shadow protected abstract boolean startAttack();
    @Shadow protected abstract void startUseItem();
    @Shadow protected abstract void continueAttack(boolean down);
    @Inject(method = "pauseIfInactive", at = @At("HEAD"), cancellable = true)
    private void neon$background(CallbackInfo ci) {
        if (Neon.CLICKER.keepRunningInBackground((Minecraft) (Object) this)) ci.cancel();
    }
    @ModifyExpressionValue(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;isMouseGrabbed()Z"))
    private boolean neon$backgroundMining(boolean grabbed) {
        return grabbed || Neon.CLICKER.keepRunningInBackground((Minecraft) (Object) this);
    }
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 10000))
    private int neon$chatScreenCooldown(int original) {
        return Neon.CLICKER.inChat((Minecraft) (Object) this) ? missTime : original;
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void neon$chatClicks(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (!Neon.CLICKER.inChat(mc)) return;
        // Chat does not run handleKeybinds. Drive only the normal attack/use methods,
        // leaving the chat field, mouse, key queues, and text input completely alone.
        var clicks = Neon.CLICKER.clicks(mc, missTime <= 0, rightClickDelay <= 0);
        if (!mc.player.isUsingItem()) {
            if (clicks.attackPress()) startAttack();
            if (clicks.use()) startUseItem();
        } else if (!clicks.use() && !mc.options.keyUse.isDown()) mc.gameMode.releaseUsingItem(mc.player);
        if (clicks.attackDown()) continueAttack(true);
        else if (!mc.options.keyAttack.isDown()) continueAttack(false);
        if (missTime > 0) missTime--;
    }
    @WrapMethod(method = "handleKeybinds")
    private void neon$input(Operation<Void> original) {
        Minecraft mc = (Minecraft) (Object) this;
        KeyMapping attack = mc.options.keyAttack, use = mc.options.keyUse;
        boolean physicalAttack = attack.isDown(), physicalUse = use.isDown();
        KeyAccessor attackAccess = (KeyAccessor) attack, useAccess = (KeyAccessor) use;
        int attackClicks = attackAccess.neon$getClicks(), useClicks = useAccess.neon$getClicks();
        try {
            // Let vanilla clear its screen-release cooldown and process manual slot/menu changes.
            boolean transition = ((KeyAccessor) mc.options.keyInventory).neon$getClicks() > 0
                || ((KeyAccessor) mc.options.keySwapOffhand).neon$getClicks() > 0
                || ((KeyAccessor) mc.options.keyDrop).neon$getClicks() > 0;
            for (KeyMapping key : mc.options.keyHotbarSlots) transition |= ((KeyAccessor) key).neon$getClicks() > 0;
            if (missTime > 10 || transition) Neon.CLICKER.resetTiming();
            else Neon.CLICKER.apply(mc, missTime <= 0, rightClickDelay <= 0);
            original.call();
        } finally {
            attack.setDown(physicalAttack); use.setDown(physicalUse);
            attackAccess.neon$setClicks(Math.min(attackClicks, attackAccess.neon$getClicks()));
            useAccess.neon$setClicks(Math.min(useClicks, useAccess.neon$getClicks()));
        }
    }
}

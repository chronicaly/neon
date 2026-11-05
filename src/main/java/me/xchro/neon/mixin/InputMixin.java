package me.xchro.neon.mixin;

import me.xchro.neon.Neon;
import net.minecraft.client.*;
import org.spongepowered.asm.mixin.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(Minecraft.class)
public abstract class InputMixin {
    @Shadow private int missTime;
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
            else Neon.CLICKER.apply(mc);
            original.call();
        } finally {
            attack.setDown(physicalAttack); use.setDown(physicalUse);
            attackAccess.neon$setClicks(Math.min(attackClicks, attackAccess.neon$getClicks()));
            useAccess.neon$setClicks(Math.min(useClicks, useAccess.neon$getClicks()));
        }
    }
}

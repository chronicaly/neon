package me.xchro.neon.mixin;

import me.xchro.neon.Neon;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class TotemPopNameMixin {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void neon$totemCount(Entity entity, CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(Neon.TOTEM_POPS.nameTag(entity, cir.getReturnValue()));
    }
}

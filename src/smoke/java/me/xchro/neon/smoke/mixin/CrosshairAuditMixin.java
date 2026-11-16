package me.xchro.neon.smoke.mixin;

import net.minecraft.client.gui.*;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class CrosshairAuditMixin {
    @Inject(method="extractCrosshair",at=@At("HEAD"))
    private void neon$background(GuiGraphicsExtractor g,DeltaTracker tracker,CallbackInfo ci){me.xchro.neon.smoke.CrosshairSmoke.underlay(g);}
}

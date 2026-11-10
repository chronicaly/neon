package me.xchro.neon.smoke.mixin;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Options.class)
public abstract class QualityOptionsMixin {
    @Inject(method="save",at=@At("HEAD"))
    private void save(CallbackInfo ci) { me.xchro.neon.smoke.LiveSmoke.optionsWrite(); }
}

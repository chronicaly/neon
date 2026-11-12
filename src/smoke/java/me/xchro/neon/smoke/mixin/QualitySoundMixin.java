package me.xchro.neon.smoke.mixin;

import me.xchro.neon.smoke.SoundAudit;
import net.minecraft.client.sounds.*;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observe sound requests without emitting audio from any verification scenario. */
@Mixin(SoundEngine.class)
abstract class QualitySoundMixin {
    @Inject(method="play",at=@At("HEAD"),cancellable=true)
    private void neon$observe(SoundInstance sound,CallbackInfoReturnable<SoundEngine.PlayResult> ci) {
        SoundAudit.played(sound);
        ci.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
    }
}

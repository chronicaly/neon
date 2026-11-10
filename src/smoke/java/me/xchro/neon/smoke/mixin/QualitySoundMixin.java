package me.xchro.neon.smoke.mixin;

import me.xchro.neon.smoke.SoundAudit;
import net.minecraft.client.sounds.*;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
abstract class QualitySoundMixin {
    @Inject(method="play",at=@At("HEAD"))
    private void neon$observe(SoundInstance sound,CallbackInfoReturnable<SoundEngine.PlayResult> ci) { SoundAudit.played(sound); }
}

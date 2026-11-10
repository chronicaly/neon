package me.xchro.neon.smoke.mixin;
import me.xchro.neon.ui.UiSounds;
import me.xchro.neon.smoke.SoundAudit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=UiSounds.class,remap=false)
abstract class NeonAudioMixin {
    @Inject(method="play(D)V",at=@At("HEAD"),require=0)
    private static void neon$ownAudio(double volume,CallbackInfo ci){SoundAudit.neonClicks++;SoundAudit.lastVolume=volume;}
}

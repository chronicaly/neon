package me.xchro.neon.smoke.mixin;
import me.xchro.neon.ui.UiSounds;
import me.xchro.neon.smoke.SoundAudit;
import me.xchro.neon.core.UiSoundPolicy.Request;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=UiSounds.class,remap=false)
abstract class NeonAudioMixin {
    // Keep volume/trigger assertions, but never send test cues to the desktop speakers.
    @Inject(method="play",at=@At("HEAD"),cancellable=true)
    private static void neon$ownAudio(Request request,CallbackInfo ci){SoundAudit.played(request);ci.cancel();}
    @Inject(method="warm",at=@At("HEAD"),cancellable=true)
    private static void neon$noTestAudioDevice(CallbackInfo ci){ci.cancel();}
}

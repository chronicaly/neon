package me.xchro.neon.smoke.mixin;

import me.xchro.neon.smoke.PerformanceSmoke;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Include native chat and its Neon hooks on both comparison JARs; nested calls count once. */
@Mixin({ChatComponent.class,ChatScreen.class})
public abstract class PerfChatMixin {
    @Inject(method="extractRenderState",at=@At("HEAD"))
    private void start(CallbackInfo ci){PerformanceSmoke.begin();}
    @Inject(method="extractRenderState",at=@At("RETURN"))
    private void end(CallbackInfo ci){PerformanceSmoke.end();}
}

package me.xchro.neon.mixin;

import net.minecraft.client.ToggleKeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ToggleKeyMapping.class)
public interface ToggleKeyAccessor {
    @Invoker("reset") void neon$resetToggle();
    @Invoker("release") void neon$releaseToggle();
}

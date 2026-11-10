package me.xchro.neon.mixin;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(KeyMapping.class)
public interface KeyAccessor {
    @Accessor("key") com.mojang.blaze3d.platform.InputConstants.Key neon$getKey();
    @Accessor("clickCount") int neon$getClicks();
    @Accessor("clickCount") void neon$setClicks(int count);
}

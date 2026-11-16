package me.xchro.neon.mixin;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(GuiGraphicsExtractor.class)
public interface ChatGraphicsAccessor {
    @Accessor("mouseX") int neon$mouseX();
    @Accessor("mouseY") int neon$mouseY();
}

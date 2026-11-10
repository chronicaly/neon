package me.xchro.neon.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.IMEPreeditOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Keep vanilla editing/IME state while Neon renders text with its own measured typeface. */
@Mixin(EditBox.class)
public interface EditBoxAccessor {
    @Accessor("highlightPos") int neon$highlight();
    @Accessor("hint") Component neon$hint();
    @Accessor("isEditable") boolean neon$editable();
    @Accessor("focusedTime") long neon$focusedTime();
    @Accessor("preeditOverlay") IMEPreeditOverlay neon$preedit();
}

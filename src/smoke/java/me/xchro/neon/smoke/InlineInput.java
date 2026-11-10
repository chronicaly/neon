package me.xchro.neon.smoke;
import me.xchro.neon.ui.ExactSlider;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
final class InlineInput {
    static ExactSlider slider(ContainerEventHandler parent) {
        for (var child : parent.children()) {
            if (child instanceof ExactSlider s && s.isEditing()) return s;
            if (child instanceof ContainerEventHandler c) { var s = slider(c); if (s != null) return s; }
        }
        return null;
    }
    static EditBox field(ContainerEventHandler parent) {
        try { var f = ExactSlider.class.getDeclaredField("edit"); f.setAccessible(true); return (EditBox) f.get(slider(parent)); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
}

package me.xchro.neon.smoke;
import me.xchro.neon.ui.ExactSlider;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
final class InlineInput {
    static void clickTrack(ExactSlider slider, boolean maximum) {
        try {
            var x = ExactSlider.class.getDeclaredMethod(maximum ? "compactTrackRight" : "compactTrackLeft"); x.setAccessible(true);
            var y = ExactSlider.class.getDeclaredMethod("compactTrackY"); y.setAccessible(true);
            var event = new net.minecraft.client.input.MouseButtonEvent((int)x.invoke(slider), (int)y.invoke(slider), new net.minecraft.client.input.MouseButtonInfo(0, 0));
            slider.mouseClicked(event, false);
            var screen = (me.xchro.neon.ui.PixelScreen) net.minecraft.client.Minecraft.getInstance().gui.screen();
            screen.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(screen.guiCoordinate(event.x()), screen.guiCoordinate(event.y()), event.buttonInfo()));
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
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

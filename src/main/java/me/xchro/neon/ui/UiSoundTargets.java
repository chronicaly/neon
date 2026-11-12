package me.xchro.neon.ui;

import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.*;

/** Matches interactive, clipped controls; module child controls share one hover identity. */
final class UiSoundTargets {
    private UiSoundTargets() {}
    static String pointer(ContainerEventHandler group, double x, double y) { return pointer(group, x, y, ""); }
    private static String pointer(ContainerEventHandler group, double x, double y, String path) {
        var children = group.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            var child = children.get(i);
            if (!(child instanceof AbstractWidget w) || !w.visible || !w.active || !w.isMouseOver(x, y)) continue;
            String key = path + "/" + WidgetMemory.identity(w);
            if (child instanceof ContainerEventHandler nested) {
                String hit = pointer(nested, x, y, key);
                if (hit != null) return child instanceof UiModuleRow || child instanceof UiModuleTile ? key : hit;
                if (child instanceof ColorPicker) return null;
            } else if (!(w instanceof StringWidget)) return key;
        }
        return null;
    }
    static String focus(ContainerEventHandler group) {
        String path = "", row = null;
        while (group.getFocused() instanceof AbstractWidget widget && widget.visible && widget.active) {
            path += "/" + WidgetMemory.identity(widget);
            if (widget instanceof UiModuleRow || widget instanceof UiModuleTile) row = path;
            if (widget instanceof ContainerEventHandler nested) group = nested;
            else return row == null ? path : row;
        }
        return null;
    }
}

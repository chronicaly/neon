package me.xchro.neon.smoke;

import me.xchro.neon.ui.SettingsScreen;
import net.minecraft.client.gui.screens.Screen;

/** Scenario isolation; real reopen tests deliberately construct SettingsScreen directly. */
final class Menus {
    static void configure(SettingsScreen screen,String name) {
        configureIn(screen,name);
    }
    private static boolean configureIn(net.minecraft.client.gui.components.events.ContainerEventHandler parent,String name) {
        for(var child:parent.children()) {
            if(child instanceof net.minecraft.client.gui.components.Button button && button.getMessage().getString().equals(name)) {
                if(parent.getClass().getSimpleName().equals("UiModuleTile"))
                    button.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(button.getX()+3,button.getY()+12,new net.minecraft.client.input.MouseButtonInfo(1,0)),false);
                else button.onPress(null);
                return true;
            }
            if(child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler group && configureIn(group,name))return true;
        }
        return false;
    }
    static void reset() {
        try {
            var field = SettingsScreen.class.getDeclaredField("NAVIGATION"); field.setAccessible(true);
            var memory = field.get(null);
            var clear = memory.getClass().getDeclaredMethod("clear"); clear.setAccessible(true); clear.invoke(memory);
        } catch (NoSuchFieldException baseline) {
            // Older comparison artifacts had no session navigation.
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    static SettingsScreen fresh(Screen parent) { reset(); return new SettingsScreen(parent); }
}

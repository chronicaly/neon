package me.xchro.neon.ui;
import com.terraformersmc.modmenu.api.*;
public final class NeonModMenu implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() { return SettingsScreen::new; }
}

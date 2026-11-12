package me.xchro.neon.ui;

import me.xchro.neon.Neon;
import me.xchro.neon.config.ProfileStore;
import me.xchro.neon.core.UiSoundPolicy.Cue;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public final class ProfilesScreen extends PixelScreen {
    private final Screen parent;
    private final SettingsSession session;
    private String draft = "", status = "";
    private boolean error;
    private int x, y, w, h;
    public ProfilesScreen(Screen parent, SettingsSession session) { super(Ui.label("Profiles")); this.parent = parent; this.session = session; }
    @Override protected void init() {
        w = Math.min(440, canvasWidth() - 32); h = Math.min(340, canvasHeight() - 24); x = (canvasWidth() - w) / 2; y = (canvasHeight() - h) / 2;
        EditBox name = addRenderableWidget(new UiEditBox(x + 24, y + 64, w - 130, "New profile name"));
        name.setMaxLength(32); name.setHint(Ui.label("Profile name")); name.setValue(draft); name.setResponder(v -> draft = v);
        addRenderableWidget(new UiButton("Save as...", x + w - 94, y + 58, 78, 26, UiButton.Kind.PRIMARY, () -> {
            try {
                String valid = ProfileStore.validName(draft);
                if (Neon.profiles.names().contains(valid)) throw new IllegalArgumentException("Name already exists. Use Update below.");
                Neon.profiles.put(valid, Neon.config); message("Saved " + valid, false); draft = ""; rebuildWidgets();
                UiSounds.cue(Cue.CONFIRM);
            } catch (IllegalArgumentException invalid) { message(invalid.getMessage(), true); }
            catch (Exception failure) { message("Could not save profile.", true); Neon.LOG.error("Profile save failed", failure); }
        }));
        LinearLayout rows = LinearLayout.vertical().spacing(6);
        for (String profile : Neon.profiles.names()) {
            LinearLayout row = LinearLayout.horizontal().spacing(4);
            row.addChild(new UiButton(profile, 0, 0, w - 154, 28, UiButton.Kind.NAV, () -> session.change(() -> {
                if (me.xchro.neon.config.ConfigStore.encode(Neon.config).equals(me.xchro.neon.config.ConfigStore.encode(Neon.profiles.get(profile)))) return;
                if (Neon.applyConfig(Neon.profiles.get(profile))) { message("Loaded " + profile, false); UiSounds.cue(Cue.CONFIRM); }
                else message("Loaded, but could not save settings.", true);
            })).hint("Load this setup. Your key bindings stay unchanged."));
            row.addChild(new UiButton("Update", 0, 0, 54, 28, () -> confirm("Update " + profile + "?", "Replace this profile with your current settings?", () -> {
                if (me.xchro.neon.config.ConfigStore.encode(Neon.profiles.get(profile)).equals(me.xchro.neon.config.ConfigStore.encode(Neon.config))) return false;
                Neon.profiles.put(profile, Neon.config); message("Updated " + profile, false); return true;
            })));
            row.addChild(new UiButton("Delete", 0, 0, 54, 28, UiButton.Kind.QUIET, () -> confirm("Delete " + profile + "?", "Remove this saved profile? Your current settings will stay as they are.", () -> {
                if (!Neon.profiles.names().contains(profile)) return false;
                Neon.profiles.delete(profile); message("Deleted " + profile, false); return true;
            })));
            rows.addChild(row);
        }
        addRenderableWidget(new UiScroll(x + 16, y + 96, w - 28, Math.max(30, h - 120), rows));
    }
    private interface FileAction { boolean run() throws Exception; }
    private void confirm(String title, String body, FileAction action) {
        minecraft.gui.setScreen(new ConfirmSheet(this, title, body, yes -> {
            if (yes) try { if (action.run()) UiSounds.cue(Cue.CONFIRM); } catch (Exception failure) { message("Could not change profile.", true); Neon.LOG.error("Profile write failed", failure); }
            else UiSounds.cue(Cue.BACK);
            minecraft.gui.setScreen(this);
        }));
    }
    private void message(String text, boolean error) { status = text == null ? "Could not save profile" : text; this.error = error; }
    @Override protected void pixelBackground(GuiGraphicsExtractor g, int mx, int my, float delta) { Ui.background(g, canvasWidth(), canvasHeight()); }
    @Override protected void renderPixels(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Ui.window(g, x, y, w, h);
        Ui.title(g, "Profiles", x + 18, y + 15);
        renderWidgets(g, mx, my, delta);
        Ui.text(g, Ui.fit(status, w - 32), x + 16, y + h - 17, error ? Ui.RED : Ui.MUTED);
    }
    @Override protected boolean pixelKeyPressed(KeyEvent e) {
        if (e.key() == GLFW.GLFW_KEY_Z && (e.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            if (session.undo()) message("Restored previous settings", false);
            return true;
        }
        return super.pixelKeyPressed(e);
    }
    @Override public void onClose() { Neon.save(); minecraft.gui.setScreen(parent); UiSounds.cue(Cue.BACK); }
    @Override public void removed() { super.removed(); session.end(); Neon.flushSaves(); }
}

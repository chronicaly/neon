package me.xchro.neon.ui;

import me.xchro.neon.Neon;
import me.xchro.neon.config.ProfileStore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;

public final class ProfilesScreen extends Screen {
    private final Screen parent;
    private final SettingsSession session;
    private String draft = "", status = "";
    private boolean error;
    private Button undo;
    public ProfilesScreen(Screen parent, SettingsSession session) {
        super(Component.literal("Neon Profiles")); this.parent = parent; this.session = session;
    }
    @Override protected void init() {
        int w = Math.min(420, width - 36), left = (width - w) / 2;
        EditBox name = addRenderableWidget(new EditBox(font, left, 48, w - 92, 20, Component.literal("New profile name")));
        name.setMaxLength(32); name.setHint(Component.literal("New profile name")); name.setValue(draft); name.setResponder(v -> draft = v);
        addRenderableWidget(Button.builder(Component.literal("Save as..."), b -> {
            try {
                String valid = ProfileStore.validName(draft);
                if (Neon.profiles.names().contains(valid)) throw new IllegalArgumentException("Name already exists. Use Update below.");
                Neon.profiles.put(valid, Neon.config); message("Saved " + valid, false); draft = ""; rebuildWidgets();
            } catch (IllegalArgumentException invalid) { message(invalid.getMessage(), true); }
            catch (Exception failure) { message("Could not save profile. Check the game log.", true); Neon.LOG.error("Profile save failed", failure); }
        }).bounds(left + w - 88, 48, 88, 20).build());
        LinearLayout rows = LinearLayout.vertical().spacing(4);
        for (String profile : Neon.profiles.names()) {
            LinearLayout row = LinearLayout.horizontal().spacing(4);
            var load = row.addChild(Button.builder(Component.literal(profile), b -> {
                session.change(() -> {
                    if (Neon.applyConfig(Neon.profiles.get(profile))) message("Loaded " + profile, false);
                    else message("Loaded, but settings could not be saved.", true);
                });
            }).bounds(0, 0, w - 130, 20).build());
            load.setTooltip(Tooltip.create(Component.literal("Load " + profile + ". Applies Neon settings and stops automation; keeps your key bindings. Undo restores the previous settings.")));
            row.addChild(Button.builder(Component.literal("Update"), b -> confirm("Update " + profile + "?", "Replace this profile with your current Neon settings?", () -> {
                Neon.profiles.put(profile, Neon.config); message("Updated " + profile, false);
            })).bounds(0, 0, 62, 20).build());
            row.addChild(Button.builder(Component.literal("Delete"), b -> confirm("Delete " + profile + "?", "Remove this saved profile? Your current settings will stay as they are.", () -> {
                Neon.profiles.delete(profile); message("Deleted " + profile, false);
            })).bounds(0, 0, 60, 20).build());
            rows.addChild(row);
        }
        ScrollableLayout scroll = new ScrollableLayout(minecraft, rows, Math.max(40, height - 130));
        scroll.setMinWidth(w); scroll.setMinHeight(Math.max(40, height - 130)); scroll.arrangeElements();
        scroll.setX((width - scroll.getWidth()) / 2); scroll.setY(78); scroll.visitWidgets(this::addRenderableWidget);
        undo = addRenderableWidget(Button.builder(Component.literal("Undo settings"), b -> { if (session.undo()) message("Restored previous settings", false); }).bounds(width / 2 - 122, height - 26, 120, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(width / 2 + 2, height - 26, 120, 20).build());
    }
    private interface FileAction { void run() throws Exception; }
    private void confirm(String title, String body, FileAction action) {
        minecraft.gui.setScreen(new ConfirmScreen(yes -> {
            if (yes) try { action.run(); } catch (Exception failure) { message("Could not change profile. Check the game log.", true); Neon.LOG.error("Profile write failed", failure); }
            minecraft.gui.setScreen(this);
        }, Component.literal(title), Component.literal(body)));
    }
    private void message(String text, boolean error) { status = text == null ? "Could not save profile" : text; this.error = error; }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        undo.active = session.canUndo();
        super.extractRenderState(g, x, y, delta);
        g.centeredText(font, title, width / 2, 12, 0xffffffff);
        g.centeredText(font, "Click a profile to load it", width / 2, 30, 0xffaaaaaa);
        if (!status.isEmpty()) g.centeredText(font, status, width / 2, height - 40, error ? 0xffff7777 : 0xffaaaaaa);
    }
    @Override public void onClose() { Neon.save(); minecraft.gui.setScreen(parent); }
    @Override public void removed() { session.end(); Neon.save(); }
}

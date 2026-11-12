package me.xchro.neon.ui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import java.util.function.*;
import me.xchro.neon.Neon;
import me.xchro.neon.core.NumberValue;
import me.xchro.neon.core.ExpandingMenu;
import me.xchro.neon.module.ClientModule;
import me.xchro.neon.core.UiSoundPolicy.Cue;
import me.xchro.neon.config.NeonConfig;
import me.xchro.neon.config.ThemeColor;
import me.xchro.neon.config.NeonConfig.LeftAction;
import me.xchro.neon.config.ProfileStore;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.layouts.*;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Equal-width module buttons and a settings drawer inside one animated shell. */
public final class SettingsScreen extends PixelScreen {
    private static final String[] TABS = {"Visuals", "Combat", "Settings"};
    private static final int ROW_HEIGHT = 27;
    private final Screen parent;
    private final SettingsSession session = new SettingsSession();
    private final MenuWindow window = new MenuWindow();
    private final ExpandingMenu expansion = new ExpandingMenu();
    private final me.xchro.neon.core.OutlineTravel outlineTravel = new me.xchro.neon.core.OutlineTravel();
    private ExpandingMenu.Frame frame;
    private final List<IntConsumer> detailWidths = new ArrayList<>();
    private UiScroll details;
    private String drawerName = "";
    private int detailLayoutWidth, detailLayoutHeight;
    private final List<Runnable> refreshers = new ArrayList<>();
    private final Map<Integer, UiButton> navigationButtons = new HashMap<>();
    private static final MenuNavigation NAVIGATION = new MenuNavigation();
    private int page, sx, sy, sw, sh, side, contentX, contentY, panelWidth;
    private String module = "", renderedView = "", error = "", profileDraft = "", expandedColor = "";
    private ColorPicker colorPicker;
    private ColorRow colorSource;
    private LinearLayout rows;
    private UiScroll scroll;
    private UiButton settingsNav, hudEditorButton;
    private KeyMapping capturing;
    private String renderedColor = "";
    private boolean colorOpen;
    private ColorPicker reusablePicker;
    SettingsSession session() { return session; }
    void relayoutControls() { if (scroll != null) scroll.relayout(); if (details != null) details.relayout(); }
    public ScreenRectangle menuBounds() { return new ScreenRectangle(sx, sy, sw, sh); }
    public ScreenRectangle expandedBounds() { return new ScreenRectangle((int)Math.round(frame.x()),sy,(int)Math.round(frame.width()),sh); }
    public ScreenRectangle moduleSettingsBounds() { return new ScreenRectangle(sx+sw+12,contentY,Math.max(1,(int)Math.round(frame.drawerWidth())-24),sh-50); }
    public SettingsScreen(Screen parent) {
        super(Ui.label("Neon")); this.parent = parent;
        page = NAVIGATION.page(); module = NAVIGATION.section(page);
    }
    private String view() { return page + "/" + module; }
    @Override protected void init() {
        if (hasPointerGesture()) finishPointerGesture();
        boolean sameView = renderedView.equals(view());
        WidgetMemory memory = renderedView.startsWith(page + "/") ? new WidgetMemory(scroll) : null;
        WidgetMemory detailMemory = sameView && details != null ? new WidgetMemory(details) : null;
        UiScroll closingDetails = details;
        boolean sameColor = renderedColor.equals(expandedColor);
        reusablePicker = sameColor ? colorPicker : null;
        session.end(); rememberScroll(); scroll = null; colorPicker = null; colorSource = null; renderedView = view(); refreshers.clear(); Ui.refreshTheme();
        NAVIGATION.select(page, module);
        window.fit(canvasWidth(), canvasHeight()); var bounds = window.bounds();
        expansion.open(!module.isEmpty(), System.nanoTime());
        frame = expansion.frame(bounds.left(),bounds.width(),canvasWidth(),System.nanoTime());
        sx = (int)Math.round(frame.x()); sy = bounds.top(); sw = (int)Math.round(frame.mainWidth()); sh = bounds.height();
        side = 96;
        int inset = 12;
        contentX = sx + side + inset; contentY = sy + 38;
        // Keep glow and keyboard focus inside the control column's breathing room.
        panelWidth = sw - side - inset * 2 - 8;
        int navRow = 0;
        for (int i : new int[]{1, 0, 2}) {
            int destination = i;
            var navigation = addRenderableWidget(new UiButton(TABS[i], sx + 6, i == 2 ? sy + sh - 60 : sy + 41 + 25 * navRow++, side - 12, 23, UiButton.Kind.NAV,
                () -> {
                    if (page == destination) return;
                    capturing = null; expandedColor = ""; colorOpen = false;
                    page = destination; module = NAVIGATION.section(destination); error = ""; rebuildWidgets();
                    UiSounds.cue(Cue.NAVIGATE);
                }).selected(() -> page == destination));
            UiButton previousNavigation = navigationButtons.put(i, navigation);
            if (previousNavigation != null) navigation.inherit(previousNavigation);
            if (i == 2) settingsNav = navigation.centeredNavigation();
        }
        rows = LinearLayout.vertical().spacing(0);
        moduleRows();
        scroll = addRenderableWidget(new UiScroll(contentX, contentY, panelWidth + 8, contentHeight(), rows));
        details = null;
        if (!module.isEmpty()) {
            drawerName = module; detailWidths.clear();
            LinearLayout gridRows = rows;
            rows = LinearLayout.vertical().spacing(0);
            controls();
            details = addWidget(new UiScroll(sx+sw+12,contentY,frame.fullDrawerWidth()-24,sh-50,rows));
            detailLayoutWidth=detailLayoutHeight=0;
            rows = gridRows;
            details.setScrollAmount(NAVIGATION.position(view() + "/details"));
            if (detailMemory != null) detailMemory.restore(details,false,true,true);
        } else details = closingDetails;
        UiButton previousHudEditor = hudEditorButton;
        hudEditorButton = addRenderableWidget(new UiButton("HUD editor", sx + 6, sy + sh - 35, side - 12, 23, UiButton.Kind.NAV, () -> {
            capturing = null;
            minecraft.gui.setScreen(new HudEditor(this));
        }).centeredNavigation().hint("Open the HUD editor."));
        if (previousHudEditor != null) hudEditorButton.inherit(previousHudEditor);
        anchorBottomControls();
        scroll.setScrollAmount(NAVIGATION.position(view()));
        if (memory != null) memory.restore(scroll, false, sameColor, true);
        if (scroll.getFocused() != null) setFocused(scroll);
        if (details != null && !module.isEmpty() && details.getFocused() != null) setFocused(details);
        layoutDrawer();
        if (colorPicker != null) {
            addWidget(colorPicker);
            colorPicker.anchor(expandedBounds(), canvasWidth(), canvasHeight());
        }
        renderedColor = expandedColor;
        refresh();
    }
    private void rememberScroll() {
        if (scroll != null) NAVIGATION.remember(renderedView, scroll.scrollAmount());
        if (details != null && !renderedView.endsWith("/")) NAVIGATION.remember(renderedView + "/details", details.scrollAmount());
    }
    private void open(String name) { capturing = null; expandedColor = ""; colorOpen = false; module = module.equals(name) ? "" : name; error = ""; rebuildWidgets(); UiSounds.cue(module.isEmpty() ? Cue.BACK : Cue.NAVIGATE); }
    private void back() { capturing = null; expandedColor = ""; colorOpen = false; module = ""; error = ""; rebuildWidgets(); UiSounds.cue(Cue.BACK); }
    private int controlWidth() { return frame.fullDrawerWidth() - 24; }
    private void controlResize(IntConsumer resize) { detailWidths.add(resize); }
    private int contentHeight() { return Math.max(30, sh - 50); }
    private void anchorBottomControls() {
        hudEditorButton.setX(sx + 6);
        hudEditorButton.setY(sy + sh - hudEditorButton.getHeight() - 12);
        settingsNav.setY(hudEditorButton.getY() - settingsNav.getHeight() - 2);
    }
    private void moduleRows() {
        var grid = rows.addChild(new ModuleGrid(() -> panelWidth + 8));
        if (page == 2) {
            for (String name : new String[]{"Colors", "Appearance", "Profiles"})
                grid.add(new UiModuleTile(name, () -> open(name), () -> module.equals(name)));
            return;
        }
        for (var entry : ClientModule.values()) if (entry.page == page) {
            var tile = new UiModuleTile(entry, () -> {
                session.change(() -> entry.enabled(Neon.config, !entry.enabled(Neon.config)));
                UiSounds.toggled(entry.enabled(Neon.config)); refresh();
            }, () -> open(entry.label), () -> module.equals(entry.label),
                () -> capturing == entry.key(), () -> { capturing = entry.key(); refresh(); }, () -> {
                    session.change(() -> entry.mode(Neon.config, entry.mode(Neon.config).next()));
                    refresh(); UiSounds.cue(Cue.CONFIRM);
                });
            grid.add(tile);
        }
    }
    private void controls() {
        var c = Neon.config;
        switch (module) {
            case "Freelook" -> slider("Distance", c.freelookDistance, 1, 16, .5, " blocks", v -> c.freelookDistance = v);
            case "Totem pops" -> slider("Red at", c.totemPopsRedAt, 2, 20, 1, " pops", v -> c.totemPopsRedAt = (int) v);
            case "Better chat" -> {
                var chat = c.betterChat;
                toggle("Message ping", () -> chat.messagePing, v -> chat.messagePing = v);
                toggle("Mention ping", () -> chat.mentionPing, v -> chat.mentionPing = v);
                slider("Ping volume", chat.volume * 100, 0, 100, 1, "%", v -> chat.volume = v / 100);
                toggle("Duplicates", () -> chat.duplicates, v -> chat.duplicates = v);
                toggle("Animation", () -> chat.animation, v -> chat.animation = v);
                enabled(slider("Duration", chat.animationMillis, 50, 600, 10, " ms", v -> chat.animationMillis = (int) v), () -> chat.animation);
                toggle("Highlight name", () -> chat.mentions, v -> chat.mentions = v);
                color("Mention color", () -> chat.mentionColor, (v, rgb) -> chat.mentionColor = v, null);
            }
            case "Zoom" -> {
                slider("Magnification", c.zoomFactor, 1, 16, .5, "x", v -> c.zoomFactor = v);
                slider("Transition", c.zoomMillis, 0, 500, 10, " ms", v -> c.zoomMillis = (int) v);
                toggle("Scroll zoom", () -> c.zoomWheel, v -> c.zoomWheel = v);
            }
            case "Motion blur" -> {
                slider("Strength", c.motionBlurStrength, 0, 100, 1, "%", v -> c.motionBlurStrength = (int) v);
                toggle("Blur the UI", () -> c.motionBlurIncludeUi, v -> c.motionBlurIncludeUi = v);
            }
            case "Autoclicker" -> {
                segments("Buttons", new String[]{"Left", "Right", "Both"}, () -> c.leftClick && c.rightClick ? 2 : c.rightClick ? 1 : 0,
                    i -> { c.leftClick = i != 1; c.rightClick = i != 0; rebuildWidgets(); });
                if (c.leftClick) {
                    segments("Left action", new String[]{"Click", "Hold"}, () -> c.leftAction == LeftAction.HOLD ? 1 : 0, i -> c.leftAction = i == 1 ? LeftAction.HOLD : LeftAction.CLICK);
                    enabled(interval("Left interval", c.leftIntervalTicks, v -> c.leftIntervalTicks = v), () -> c.leftAction == LeftAction.CLICK);
                }
                if (c.rightClick) interval("Right interval", c.rightIntervalTicks, v -> c.rightIntervalTicks = v);
                enabled(toggle("Entities only", () -> c.clickerEntitiesOnly, v -> c.clickerEntitiesOnly = v), () -> c.leftClick && c.leftAction == LeftAction.CLICK);
            }
            case "Colors" -> {
                var t = c.theme;
                color("Theme", () -> t.themeColor, (v,rgb) -> t.themeColor(v), null);
                toggle("Rainbow", () -> t.rainbow, t::rainbow);
                enabled(slider("Cycle",t.rainbowSeconds,1,30,1," s",v->t.rainbowSeconds=v), () -> t.rainbow);
                enabled(slider("Saturation",t.rainbowSaturation*100,0,100,1,"%",v->t.rainbowSaturation=v/100), () -> t.rainbow);
                enabled(slider("Brightness",t.rainbowBrightness*100,1,100,1,"%",v->t.rainbowBrightness=v/100), () -> t.rainbow);
                for (ThemeColor role : ThemeColor.values()) color(role.label, () -> t.color(role), (v,rgb) -> t.color(role,v,rgb), role);
                slider("Opacity",t.opacity*100,40,100,1,"%",v->t.opacity=v/100);
            }
            case "Appearance" -> {
                var t = c.theme;
                slider("UI sounds",t.soundVolume*100,0,100,1,"%",v->t.soundVolume=v/100);
                segments("Effects", new String[]{"Adaptive", "Full"}, () -> t.effects.ordinal(), i -> t.effects = NeonConfig.Effects.values()[i]);
            }
            case "Profiles" -> profiles();
        }
    }
    private <T extends LayoutElement> T row(String label, T control) {
        UiLabel text = new UiLabel(label, controlWidth(), ROW_HEIGHT, Ui.TEXT);
        rows.addChild(new ControlRow(text, control, this::controlWidth)); return control;
    }
    private UiButton toggle(String label, BooleanSupplier value, Consumer<Boolean> set) {
        UiButton button = row(label, new UiButton("", 0, 0, 24, ROW_HEIGHT, UiButton.Kind.SWITCH, () -> {
            session.change(() -> set.accept(!value.getAsBoolean()));
            UiSounds.toggled(value.getAsBoolean());
            refresh();
        }).hint(label));
        refreshers.add(() -> {
            String next = value.getAsBoolean() ? "ON" : "OFF";
            if (!button.getMessage().getString().equals(next)) button.setMessage(Ui.label(next));
        }); return button;
    }
    private void segments(String label, String[] values, IntSupplier selected, IntConsumer set) {
        FlowRow choices = new FlowRow(this::controlWidth, 1);
        for (int i = 0; i < values.length; i++) {
            int index = i;
            int width = UiFont.width(values[i], 10) + 12;
            choices.add(new UiButton(values[i], 0, 0, width, 21, UiButton.Kind.SEGMENT,
                () -> { if (selected.getAsInt() == index) return; session.change(() -> set.accept(index)); if (label.equals("Buttons")) rebuildWidgets(); else refresh(); UiSounds.cue(Cue.CONFIRM); }).selected(() -> selected.getAsInt() == index).hint(label));
        }
        choices.arrangeElements(); row(label, choices);
    }
    private void enabled(AbstractWidget widget, BooleanSupplier enabled) { refreshers.add(() -> widget.active = enabled.getAsBoolean()); }
    static String bindingName(KeyMapping key) {
        if (key.isUnbound()) return "[—]";
        InputConstants.Key input = InputConstants.getKey(key.saveString());
        if (input.getType() == InputConstants.Type.MOUSE) return "[M" + (input.getValue() + 1) + "]";
        String name = key.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT)
            .replace("LEFT ", "").replace("RIGHT ", "").replace("CONTROL", "CTRL").replace("SHIFT", "SHFT")
            .replace("INSERT", "INS").replace("DELETE", "DEL").replace("BACKSPACE", "BKSP")
            .replace("PAGE UP", "PGUP").replace("PAGE DOWN", "PGDN").replace("CAPS LOCK", "CAPS")
            .replace("NUM LOCK", "NUM").replace("SCROLL LOCK", "SCRL").replace("PRINT SCREEN", "PRTSC")
            .replace("KEYPAD ", "NUM ");
        return "[" + name + "]";
    }
    private ExactSlider slider(String label, double current, double min, double max, double step, String suffix, DoubleConsumer set) {
        var slider = new ExactSlider(this, session, label, controlWidth(), current, min, max, step, n -> NumberValue.format(n) + suffix, set).identity(view() + "/" + label);
        rows.addChild(slider.compactRow());
        controlResize(slider::setWidth); return slider;
    }
    private ExactSlider interval(String label, int current, IntConsumer set) {
        var slider = rows.addChild(new ExactSlider(this, session, label, controlWidth(), current, 0, 60, 1,
            n -> n == 0 ? "Vanilla (0)" : (int) n + (n == 1 ? " tick" : " ticks"), v -> set.accept((int) v)).compactRow());
        controlResize(slider::setWidth); return slider;
    }
    private void color(String name, IntSupplier value, BiConsumer<Integer, Boolean> set, ThemeColor role) {
        var row = rows.addChild(new ColorRow(name, controlWidth(), value, () -> openColor(name), () -> colorOpen && expandedColor.equals(name)));
        controlResize(row::setWidth);
        if (expandedColor.equals(name)) {
            colorSource = row;
            colorPicker = reusablePicker != null ? reusablePicker : new ColorPicker(name, value.getAsInt(), session, value, set);
            colorPicker.bind(value, set);
            colorPicker.syncRole(role);
        }
    }
    private void openColor(String name) {
        capturing = null;
        if (expandedColor.equals(name) && colorPicker != null) {
            closeColor(); return;
        }
        if (colorPicker != null) colorPicker.finish();
        session.end(); expandedColor = name; colorOpen = true; rebuildWidgets();
        UiSounds.cue(Cue.NAVIGATE);
    }
    private void closeColor() {
        boolean wasOpen = colorPicker != null;
        if (hasPointerGesture()) finishPointerGesture();
        if (colorPicker != null) {
            colorPicker.finish();
            if (getFocused() == colorPicker) setFocused(details);
            removeWidget(colorPicker);
        }
        colorPicker = reusablePicker = null; colorSource = null; colorOpen = false; expandedColor = renderedColor = "";
        if (wasOpen) UiSounds.cue(Cue.BACK);
    }
    private void profiles() {
        for (String name : Neon.profiles.names()) {
            FlowRow line = new FlowRow(this::controlWidth,3);
            int updateWidth = UiFont.width("Update", 10) + 16, deleteWidth = UiFont.width("Delete", 10) + 16;
            int preferred = Ui.width(name)+18;
            var profile = line.add(new UiButton(name, 0, 0, Math.min(controlWidth(),Math.max(preferred,controlWidth()-updateWidth-deleteWidth-6)), 29, UiButton.Kind.CARD, () -> {
                var next = Neon.profiles.get(name);
                if (me.xchro.neon.config.ConfigStore.encode(next).equals(me.xchro.neon.config.ConfigStore.encode(Neon.config))) return;
                session.change(() -> { if (!Neon.applyConfig(next)) error = "Could not save settings."; else UiSounds.cue(Cue.CONFIRM); }); rebuildWidgets();
            }).hint("Load profile " + name).wrapText());
            controlResize(width -> profile.setWidth(Math.min(width, Math.max(preferred, width-updateWidth-deleteWidth-6))));
            line.add(new UiButton("Update", 0, 0, updateWidth, 29, UiButton.Kind.QUIET, () -> confirm("Update " + name + "?", "Replace this profile with your current settings?", () -> {
                if (me.xchro.neon.config.ConfigStore.encode(Neon.profiles.get(name)).equals(me.xchro.neon.config.ConfigStore.encode(Neon.config))) return false;
                Neon.profiles.put(name, Neon.config); return true;
            })).small());
            line.add(new UiButton("Delete", 0, 0, deleteWidth, 29, UiButton.Kind.QUIET, () -> confirm("Delete " + name + "?", "Remove this saved profile?", () -> {
                if (!Neon.profiles.names().contains(name)) return false;
                Neon.profiles.delete(name); return true;
            })).small());
            rows.addChild(line);
        }
        FlowRow form = new FlowRow(this::controlWidth, 6);
        FrameLayout nameFrame = new FrameLayout(profileNameWidth(controlWidth()), 39);
        UiEditBox name = new UiEditBox(0, 0, profileNameWidth(controlWidth()) - 14, "Profile name");
        name.setMaxLength(32); name.setHint(Ui.label("Profile name")); name.setValue(profileDraft); name.setResponder(value -> profileDraft = value);
        controlResize(width -> { nameFrame.setMinWidth(profileNameWidth(width)); name.setWidth(profileNameWidth(width) - 14); });
        nameFrame.addChild(name, nameFrame.newChildLayoutSettings().paddingLeft(7).paddingTop(13));
        form.add(nameFrame);
        FrameLayout saveFrame = form.add(new FrameLayout(55, 39));
        saveFrame.addChild(new UiButton("Save", 0, 0, 55, 26, () -> {
            try {
                String valid = ProfileStore.validName(profileDraft);
                if (Neon.profiles.names().contains(valid)) throw new IllegalArgumentException("Name already exists.");
                Neon.profiles.put(valid, Neon.config); profileDraft = ""; error = ""; rebuildWidgets();
                UiSounds.cue(Cue.CONFIRM);
            } catch (IllegalArgumentException invalid) { error = invalid.getMessage(); }
            catch (Exception failure) { error = "Could not save profile."; Neon.LOG.error(error, failure); }
        }), saveFrame.newChildLayoutSettings().paddingTop(7));
        rows.addChild(form);
    }
    private int profileNameWidth(int width) { return width >= 125 ? width - 61 : width; }
    private interface FileAction { boolean run() throws Exception; }
    private void confirm(String title, String body, FileAction action) {
        minecraft.gui.setScreen(new ConfirmSheet(this, title, body, yes -> {
            if (yes) try { if (action.run()) UiSounds.cue(Cue.CONFIRM); error = ""; } catch (Exception failure) { error = "Could not change profile."; Neon.LOG.error(error, failure); }
            else UiSounds.cue(Cue.BACK);
            minecraft.gui.setScreen(this);
        }));
    }
    private void refresh() { refreshers.forEach(Runnable::run); }
    private void assign(InputConstants.Key key) {
        boolean changed = !capturing.saveString().equals(key.getName());
        session.change(() -> { capturing.setKey(key); capturing = null; KeyMapping.resetMapping(); minecraft.options.save(); }); rebuildWidgets();
        if (changed) UiSounds.cue(Cue.CONFIRM);
    }
    @Override protected boolean pixelKeyPressed(KeyEvent e) {
        if (cancelNumberDrag(e)) return true;
        if (colorPicker != null && colorPicker.editingHex() &&
            (e.key() == GLFW.GLFW_KEY_ESCAPE || e.key() == GLFW.GLFW_KEY_ENTER || e.key() == GLFW.GLFW_KEY_KP_ENTER)) return colorPicker.keyPressed(e);
        ExactSlider edit = activeNumber();
        if (edit != null) return edit.keyPressed(e);
        if (capturing != null) {
            if (e.key() == GLFW.GLFW_KEY_ESCAPE) { capturing = null; refresh(); }
            else assign(e.key() == GLFW.GLFW_KEY_BACKSPACE || e.key() == GLFW.GLFW_KEY_DELETE ? InputConstants.UNKNOWN : InputConstants.getKey(e));
            return true;
        }
        if (e.key() == GLFW.GLFW_KEY_ESCAPE && window.dragging()) { window.cancel(); layoutWindow(); return true; }
        if (e.key() == GLFW.GLFW_KEY_ESCAPE && colorOpen) { closeColor(); return true; }
        if (e.key() == GLFW.GLFW_KEY_ESCAPE && !module.isEmpty()) { back(); return true; }
        if (e.key() == GLFW.GLFW_KEY_Z && (e.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) { if (session.undo()) rebuildWidgets(); return true; }
        // Gameplay key mappings do not receive presses while a screen is open.
        // Let text fields and bind capture keep their input before toggling the menu.
        if ((Neon.settings.matches(e) || Neon.settingsAlternate.matches(e)) && !editingText()) { onClose(); return true; }
        boolean handled = super.pixelKeyPressed(e);
        if (getFocused() == details && details != null && !module.isEmpty()) { expansion.reveal(); layoutWindow(); }
        return handled;
    }
    private boolean editingText() {
        var focused = getFocused();
        while (focused instanceof ContainerEventHandler group) focused = group.getFocused();
        return focused instanceof EditBox box && box.isFocused();
    }
    @Override protected boolean pixelMouseClicked(MouseButtonEvent e, boolean twice) {
        if (capturing != null) { assign(InputConstants.Type.MOUSE.getOrCreate(e.button())); return true; }
        if (Neon.settings.matchesMouse(e) || Neon.settingsAlternate.matchesMouse(e)) { onClose(); return true; }
        if (colorPicker != null) {
            if (colorPicker.isMouseOver(e.x(),e.y())) {
                colorPicker.mouseClicked(e, twice); setFocused(colorPicker); setDragging(e.button() == 0);
                if (e.button() == 0 && colorPicker.draggingPointer()) capturePointer(colorPicker, e);
                return true;
            }
            // The selected row handles its own toggle; other outside clicks dismiss before dispatch.
            boolean selectedRow = (scroll.isMouseOver(e.x(),e.y()) || details!=null&&!module.isEmpty()&&details.isMouseOver(e.x(),e.y())) && colorSource != null && colorSource.isMouseOver(e.x(),e.y());
            if (!selectedRow) closeColor();
        }
        if (e.button() == 0) {
            int corner = shellCorner(e.x(),e.y());
            if (corner >= 0) { window.beginVisible(corner, e.x(), e.y(),sx); return true; }
        }
        if (super.pixelMouseClicked(e, twice)) return true;
        if (e.button() == 0 && e.x() >= frame.x() && e.x() < frame.x()+frame.width() && e.y() >= sy && e.y() < sy + 37) {
            window.beginVisible(4, e.x(), e.y(),sx); return true;
        }
        return false;
    }
    @Override protected boolean pixelMouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (window.dragging()) { window.drag(e.x(), e.y()); layoutWindow(); return true; }
        return super.pixelMouseDragged(e, dx, dy);
    }
    private void layoutWindow() {
        var b = window.bounds();
        frame = expansion.frame(b.left(),b.width(),canvasWidth(),System.nanoTime());
        int x=(int)Math.round(frame.x()), w=(int)Math.round(frame.mainWidth());
        int dx=x-sx,dy=b.top()-sy;
        boolean resized = sw != w || sh != b.height();
        sx = x; sy = b.top(); sw = w; sh = b.height();
        contentX = sx + side + 12; contentY = sy + 38; panelWidth = sw - side - 32;
        for (var child : children()) if (child instanceof AbstractWidget widget) { widget.setX(widget.getX() + dx); widget.setY(widget.getY() + dy); }
        anchorBottomControls();
        if (resized && scroll != null) {
            scroll.setWidth(panelWidth + 8); scroll.setHeight(contentHeight()); scroll.relayout();
        }
        if (colorPicker != null) colorPicker.anchor(expandedBounds(), canvasWidth(), canvasHeight());
        layoutDrawer();
    }
    private void layoutDrawer() {
        if (details==null) return;
        details.setX(sx+sw+12); details.setY(contentY);
        details.setWidth(Math.max(1,(int)Math.round(frame.drawerWidth())-24)); details.setHeight(sh-50);
        if(detailLayoutWidth!=frame.fullDrawerWidth() || detailLayoutHeight!=sh) {
            detailLayoutWidth=frame.fullDrawerWidth(); detailLayoutHeight=sh;
            detailWidths.forEach(change -> change.accept(controlWidth()));
            details.relayout();
        }
    }
    private int shellCorner(double x,double y) {
        double left=frame.x(),right=left+frame.width();
        if(Math.abs(x-left)<=6){if(Math.abs(y-sy)<=6)return 0;if(Math.abs(y-sy-sh)<=6)return 2;}
        if(Math.abs(x-right)<=6){if(Math.abs(y-sy)<=6)return 1;if(Math.abs(y-sy-sh)<=6)return 3;}
        return -1;
    }
    @Override protected boolean pixelMouseReleased(MouseButtonEvent e) {
        if (window.dragging()) { if (e.button() == 0) window.finish(); return true; }
        return super.pixelMouseReleased(e);
    }
    @Override protected boolean pixelMouseScrolled(double x, double y, double dx, double dy) {
        if (colorPicker != null && colorPicker.isMouseOver(x,y)) return true;
        return super.pixelMouseScrolled(x,y,dx,dy);
    }
    @Override protected void pixelBackground(GuiGraphicsExtractor g, int mx, int my, float delta) { Ui.background(g, canvasWidth(), canvasHeight()); }
    @Override protected void renderPixels(GuiGraphicsExtractor g, int mx, int my, float delta) {
        layoutWindow(); refresh();
        UiEffects.gradient(g,(float)frame.x(),sy,(float)frame.width(),sh,Ui.PANEL,Ui.PANEL,Ui.PANEL,Ui.PANEL);
        UiEffects.surfaceLight(g,sx+1,sy+1,(int)Math.round(frame.width())-2,sh-2,Ui.PANEL);
        Ui.rect(g, sx + 1, sy + 1, side - 1, sh - 2, Ui.SIDEBAR);
        Ui.rect(g, sx + side - 1, sy + 1, 1, sh - 2, UiEffects.mix(Ui.PANEL, 0xffffffff, .027));
        int brandX = sx + 15;
        for (char letter : "NEON".toCharArray()) { String glyph = Character.toString(letter); Ui.title(g, glyph, brandX, sy + 14); brandX += UiFont.width(glyph, 13) + 2; }
        Ui.title(g, TABS[page], contentX, sy + 14);
        renderWidgets(g, mx, my, delta);
        if (details!=null && frame.drawerWidth()>1) {
            Ui.clip(g,sx+sw,sy+1,(int)Math.floor(frame.x()+frame.width()-1),sy+sh-1);
            Ui.title(g,drawerName,sx+sw+12,sy+14);
            details.extractRenderState(g,module.isEmpty()?-10000:mx,module.isEmpty()?-10000:my,delta);
            g.disableScissor();
        }
        if (!error.isEmpty()) {
            boolean drawer = !module.isEmpty() && frame.drawerWidth() > 24;
            Ui.small(g, Ui.fit(error, drawer ? controlWidth() : panelWidth), drawer ? sx + sw + 12 : contentX, sy + sh - 26, Ui.RED);
        }
        // One pixel-snapped mesh surrounds both panes; its clock never restarts on retarget.
        UiEffects.outline(g,(float)frame.x(),sy,(float)frame.width(),sh,outlineTravel);
        if (colorPicker != null) colorPicker.extractRenderState(g, mx, my, delta);
    }
    @Override public void onClose() {
        if (window.dragging()) { window.cancel(); layoutWindow(); return; }
        if (Neon.save()) { minecraft.gui.setScreen(parent); UiSounds.cue(Cue.BACK); } else error = "Could not save. Check the game log.";
    }
    @Override protected boolean hasPointerGesture() { return super.hasPointerGesture() || window.dragging(); }
    @Override protected void finishPointerGesture() {
        super.finishPointerGesture();
        if (window.dragging()) window.finish();
    }
    @Override public void removed() {
        super.removed(); rememberScroll(); capturing = null;
        if (colorPicker != null) colorPicker.finish();
        expandedColor = renderedColor = profileDraft = ""; colorOpen = false; colorPicker = reusablePicker = null;
        scroll = null; details = null; session.end(); Neon.flushSaves();
    }
}

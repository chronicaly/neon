package me.xchro.neon.ui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import java.util.function.*;
import me.xchro.neon.Neon;
import me.xchro.neon.core.NumberValue;
import me.xchro.neon.config.NeonConfig;
import me.xchro.neon.config.NeonConfig.LeftAction;
import me.xchro.neon.config.ProfileStore;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.*;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Compact category rows with settings expanded in place beneath their module. */
public final class SettingsScreen extends PixelScreen {
    private static final String[] TABS = {"Visuals", "Combat", "HUD", "Settings"};
    private static final int ROW_HEIGHT = 27;
    private final Screen parent;
    private final SettingsSession session = new SettingsSession();
    private final MenuWindow window = new MenuWindow();
    private final List<Runnable> refreshers = new ArrayList<>();
    private final List<UiLabel> rowLabels = new ArrayList<>();
    private final List<IntConsumer> widths = new ArrayList<>();
    private final Map<String, Double> positions = new HashMap<>();
    private int page, sx, sy, sw, sh, side, contentX, contentY, panelWidth;
    private String module = "", renderedView = "", error = "", profileDraft = "", expandedColor = "";
    private ColorPicker colorPicker;
    private LinearLayout rows;
    private UiScroll scroll;
    private KeyMapping capturing;
    private String renderedColor = "";
    private boolean animateRows;
    private boolean colorOpen, revealColor;
    private ColorPicker reusablePicker;
    SettingsSession session() { return session; }
    public ScreenRectangle menuBounds() { return window.bounds(); }
    public SettingsScreen(Screen parent) { super(Ui.label("Neon")); this.parent = parent; }
    private String view() { return page + "/" + module; }
    @Override protected void init() {
        boolean sameView = renderedView.equals(view());
        WidgetMemory memory = renderedView.startsWith(page + "/") ? new WidgetMemory(scroll) : null;
        boolean sameColor = renderedColor.equals(expandedColor);
        reusablePicker = sameColor ? colorPicker : null;
        session.end(); rememberScroll(); scroll = null; colorPicker = null; renderedView = view(); refreshers.clear(); rowLabels.clear(); widths.clear(); Ui.refreshTheme();
        window.fit(canvasWidth(), canvasHeight()); var bounds = window.bounds();
        sx = bounds.left(); sy = bounds.top(); sw = bounds.width(); sh = bounds.height();
        side = 96;
        int inset = 12;
        contentX = sx + side + inset; contentY = sy + 38;
        // Keep glow and keyboard focus inside the control column's breathing room.
        panelWidth = sw - side - inset * 2 - 8;
        int navRow = 0;
        for (int i : new int[]{1, 0, 2, 3}) {
            int destination = i;
            addRenderableWidget(new UiButton(TABS[i], sx + 8, sy + 41 + 25 * navRow++ + (i==3?12:0), side - 16, 23, UiButton.Kind.NAV,
                () -> {
                    capturing = null; expandedColor = ""; colorOpen = revealColor = false;
                    if (destination == 2) { minecraft.gui.setScreen(new HudEditor(this)); return; }
                    page = destination; module = ""; error = ""; rebuildWidgets();
                }).selected(() -> page == destination));
        }
        rows = LinearLayout.vertical().spacing(0);
        moduleRows();
        scroll = addRenderableWidget(new UiScroll(contentX, contentY, panelWidth + 8, Math.max(30, sh - 50), rows));
        scroll.setScrollAmount(positions.getOrDefault(view(), 0.0));
        if (memory != null) memory.restore(scroll, animateRows, sameColor, sameView);
        if (scroll.getFocused() != null) setFocused(scroll);
        animateRows = false; renderedColor = expandedColor;
        refresh();
    }
    private void rememberScroll() { if (scroll != null) positions.put(renderedView, scroll.scrollAmount()); }
    private void open(String name) { capturing = null; expandedColor = ""; colorOpen = revealColor = false; module = module.equals(name) ? "" : name; error = ""; animateRows = true; rebuildWidgets(); }
    private void back() { capturing = null; expandedColor = ""; colorOpen = revealColor = false; module = ""; error = ""; rebuildWidgets(); }
    private int controlWidth() { return panelWidth - 26; }
    private void moduleRows() {
        String[] names = switch (page) {
            case 0 -> new String[]{"Freelook", "Zoom", "Motion blur"};
            case 1 -> new String[]{"Autoclicker"};
            case 2 -> new String[0];
            default -> new String[]{"Appearance", "Profiles"};
        };
        for (String name : names) {
            KeyMapping key = switch (name) { case "Freelook" -> Neon.freelook; case "Zoom" -> Neon.zoom; case "Autoclicker" -> Neon.autoclicker; default -> null; };
            var row = rows.addChild(new UiModuleRow(name, panelWidth, () -> open(name), key == null ? null : bindButton(key, 26), moduleToggle(name), () -> module.equals(name)));
            widths.add(row::setWidth);
            if (module.equals(name)) {
                LinearLayout list = rows;
                rows = LinearLayout.vertical().spacing(0);
                controls();
                list.addChild(new InsetLayout(rows, 26)); rows = list;
            }
        }
    }
    private UiButton moduleToggle(String name) {
        BooleanSupplier enabled = switch (name) {
            case "Freelook" -> () -> Neon.config.freelookEnabled;
            case "Zoom" -> () -> Neon.config.zoomEnabled;
            case "Motion blur" -> () -> Neon.config.motionBlurEnabled;
            case "Autoclicker" -> () -> Neon.config.clickerEnabled;
            default -> null;
        };
        if (enabled == null) return null;
        UiButton button = new UiButton("", 0, 0, 26, 26, UiButton.Kind.MODULE_TOGGLE, () -> {
            session.change(() -> {
                boolean next = !enabled.getAsBoolean();
                switch (name) {
                    case "Freelook" -> Neon.config.freelookEnabled = next;
                    case "Zoom" -> Neon.config.zoomEnabled = next;
                    case "Motion blur" -> Neon.config.motionBlurEnabled = next;
                    case "Autoclicker" -> Neon.config.clickerEnabled = next;
                }
                if (next) UiSounds.moduleEnabled();
            }); refresh();
        }).hint("Enable " + name);
        refreshers.add(() -> { String next=enabled.getAsBoolean()?"ON":"OFF"; if(!next.equals(button.getMessage().getString()))button.setMessage(Ui.label(next)); });
        return button;
    }
    private void controls() {
        var c = Neon.config;
        switch (module) {
            case "Freelook" -> binding("Bind", Neon.freelook);
            case "Zoom" -> {
                segments("Activation", new String[]{"Hold", "Toggle"}, () -> c.zoomToggle ? 1 : 0, i -> c.zoomToggle = i == 1);
                slider("Magnification", c.zoomFactor, 1, 16, .5, "x", v -> c.zoomFactor = v);
                slider("Transition", c.zoomMillis, 0, 500, 10, " ms", v -> c.zoomMillis = (int) v);
                toggle("Scroll zoom", () -> c.zoomWheel, v -> c.zoomWheel = v);
                reset(() -> { var d = new NeonConfig(); c.zoomFactor = d.zoomFactor; c.zoomMillis = d.zoomMillis; c.zoomToggle = d.zoomToggle; c.zoomWheel = d.zoomWheel; c.zoomScrollStep = d.zoomScrollStep; c.zoomSensitivity = d.zoomSensitivity; });
            }
            case "Motion blur" -> {
                enabled(slider("Strength", c.motionBlurStrength, 0, 100, 1, "%", v -> c.motionBlurStrength = (int) v), () -> c.motionBlurEnabled);
                enabled(toggle("Blur the UI", () -> c.motionBlurIncludeUi, v -> c.motionBlurIncludeUi = v), () -> c.motionBlurEnabled);
                reset(() -> { c.motionBlurEnabled = false; c.motionBlurStrength = 75; c.motionBlurIncludeUi = true; });
            }
            case "Autoclicker" -> {
                segments("Activation", new String[]{"Hold", "Toggle"}, () -> c.toggleClicker ? 1 : 0, i -> c.toggleClicker = i == 1);
                segments("Buttons", new String[]{"Left", "Right", "Both"}, () -> c.leftClick && c.rightClick ? 2 : c.rightClick ? 1 : 0,
                    i -> { c.leftClick = i != 1; c.rightClick = i != 0; rebuildWidgets(); });
                if (c.leftClick) {
                    segments("Left action", new String[]{"Click", "Hold"}, () -> c.leftAction == LeftAction.HOLD ? 1 : 0, i -> c.leftAction = i == 1 ? LeftAction.HOLD : LeftAction.CLICK);
                    enabled(interval("Left interval", c.leftIntervalTicks, v -> c.leftIntervalTicks = v), () -> c.leftAction == LeftAction.CLICK);
                }
                if (c.rightClick) interval("Right interval", c.rightIntervalTicks, v -> c.rightIntervalTicks = v);
                enabled(toggle("Entities only", () -> c.clickerEntitiesOnly, v -> c.clickerEntitiesOnly = v), () -> c.leftClick && c.leftAction == LeftAction.CLICK); reset(c::resetClicker);
            }
            case "Appearance" -> {
                var t = c.theme;
                color("Background", () -> t.background, v -> t.background = v);
                color("Outline", () -> t.outline, v -> t.outline = v);
                color("Accent", () -> t.accent, v -> t.accent = v);
                color("Text", () -> t.text, v -> t.text = v);
                color("Cards", () -> t.cards, v -> t.cards = v);
                color("Secondary outline", () -> t.secondary, v -> t.secondary = v);
                color("Muted text", () -> t.muted, v -> t.muted = v);
                slider("Opacity",t.opacity*100,40,100,1,"%",v->t.opacity=v/100);
                slider("Module sound",t.soundVolume*100,0,100,1,"%",v->t.soundVolume=v/100);
                segments("Effects", new String[]{"Adaptive", "Full"}, () -> t.effects.ordinal(), i -> t.effects = NeonConfig.Effects.values()[i]);
                reset(() -> c.theme = new NeonConfig.Theme());
            }
            case "Profiles" -> profiles();
        }
    }
    private void reset(Runnable reset) {
        rows.addChild(new UiButton("Reset", 0, 0, 55, 24, UiButton.Kind.QUIET, () -> { session.change(reset); rebuildWidgets(); }).hint("Restore this module's defaults. Undo restores your previous settings."));
    }
    private <T extends LayoutElement> T row(String label, T control) {
        UiLabel text = new UiLabel(label, controlWidth(), ROW_HEIGHT, Ui.TEXT);
        rowLabels.add(text);
        rows.addChild(new ControlRow(text, control, this::controlWidth)); return control;
    }
    private UiButton toggle(String label, BooleanSupplier value, Consumer<Boolean> set) {
        UiButton button = row(label, new UiButton("", 0, 0, 24, ROW_HEIGHT, UiButton.Kind.SWITCH, () -> {
            session.change(() -> set.accept(!value.getAsBoolean()));
            if(label.equals("Enabled") && value.getAsBoolean()) UiSounds.moduleEnabled();
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
                () -> { session.change(() -> set.accept(index)); if (label.equals("Buttons")) rebuildWidgets(); else refresh(); }).selected(() -> selected.getAsInt() == index).hint(label));
        }
        choices.arrangeElements(); row(label, choices);
    }
    private void enabled(AbstractWidget widget, BooleanSupplier enabled) { refreshers.add(() -> widget.active = enabled.getAsBoolean()); }
    private static String bindingName(KeyMapping key) {
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
    private UiButton bindButton(KeyMapping key, int height) {
        String label = capturing == key ? "[…]" : bindingName(key);
        int bindWidth = Math.max(19, Ui.width(label) + 4);
        UiButton button = new UiButton(label, 0, 0, bindWidth, height, UiButton.Kind.KEY, () -> { capturing = key; refresh(); }).hint("Set " + key.getName() + ". Escape cancels; Backspace clears.");
        String[] previous = {""}; boolean[] wasCapturing = {false};
        refreshers.add(() -> {
            String current = key.saveString(); boolean capture = capturing == key;
            if (current.equals(previous[0]) && capture == wasCapturing[0]) return;
            previous[0] = current; wasCapturing[0] = capture;
            boolean conflict = !key.isUnbound() && Arrays.stream(minecraft.options.keyMappings).anyMatch(other -> other != key && key.same(other));
            button.setMessage(Ui.label(capturing == key ? "[…]" : bindingName(key)).withStyle(s -> s.withColor(capturing == key ? Ui.BLUE : conflict ? Ui.RED : Ui.MUTED)));
            button.setWidth(Math.max(19, Ui.width(button.getMessage().getString()) + 4));
        }); return button;
    }
    private void binding(String label, KeyMapping key) { row(label, bindButton(key, ROW_HEIGHT)); }
    private ExactSlider slider(String label, double current, double min, double max, double step, String suffix, DoubleConsumer set) {
        var slider = rows.addChild(new ExactSlider(this, session, label, controlWidth(), current, min, max, step, n -> NumberValue.format(n) + suffix, set).fullRow());
        widths.add(w -> slider.setWidth(w - 26)); return slider;
    }
    private ExactSlider interval(String label, int current, IntConsumer set) {
        var slider = rows.addChild(new ExactSlider(this, session, label, controlWidth(), current, 0, 60, 1,
            n -> n == 0 ? "Vanilla (0)" : (int) n + (n == 1 ? " tick" : " ticks"), v -> set.accept((int) v)).fullRow());
        widths.add(w -> slider.setWidth(w - 26)); return slider;
    }
    private void color(String name, IntSupplier value, IntConsumer set) {
        var row = rows.addChild(new ColorRow(name, controlWidth(), value, () -> openColor(name)));
        widths.add(w -> row.setWidth(w - 26));
        if (expandedColor.equals(name)) {
            colorPicker = reusablePicker != null ? reusablePicker : new ColorPicker(controlWidth(), Math.max(30, sh - 50) - ROW_HEIGHT, value.getAsInt(), session, value, set);
            colorPicker.bind(value, set); colorPicker.resize(controlWidth(), Math.max(30, sh - 50) - ROW_HEIGHT); colorPicker.open(colorOpen);
            rows.addChild(colorPicker);
        }
    }
    private void openColor(String name) {
        capturing = null;
        if (expandedColor.equals(name) && colorPicker != null) {
            colorOpen = !colorOpen; colorPicker.open(colorOpen); revealColor = colorOpen; return;
        }
        session.end(); expandedColor = name; colorOpen = revealColor = true; rebuildWidgets();
    }
    private void profiles() {
        for (String name : Neon.profiles.names()) {
            FlowRow line = new FlowRow(this::controlWidth,3);
            int updateWidth = UiFont.width("Update", 10) + 16, deleteWidth = UiFont.width("Delete", 10) + 16;
            int preferred = Ui.width(name)+18;
            var profile = line.add(new UiButton(name, 0, 0, Math.min(controlWidth(),Math.max(preferred,controlWidth()-updateWidth-deleteWidth-6)), 29, UiButton.Kind.CARD, () -> {
                session.change(() -> { if (!Neon.applyConfig(Neon.profiles.get(name))) error = "Could not save settings."; }); rebuildWidgets();
            }).hint("Load profile " + name).wrapText());
            widths.add(width -> profile.setWidth(Math.min(width-26,Math.max(preferred,width-26-updateWidth-deleteWidth-6))));
            line.add(new UiButton("Update", 0, 0, updateWidth, 29, UiButton.Kind.QUIET, () -> confirm("Update " + name + "?", "Replace this profile with your current settings?", () -> Neon.profiles.put(name, Neon.config))).small());
            line.add(new UiButton("Delete", 0, 0, deleteWidth, 29, UiButton.Kind.QUIET, () -> confirm("Delete " + name + "?", "Remove this saved profile?", () -> Neon.profiles.delete(name))).small());
            rows.addChild(line);
        }
        FrameLayout form = new FrameLayout(controlWidth(), 39);
        UiEditBox name = new UiEditBox(0, 0, controlWidth() - 83, "Profile name");
        name.setMaxLength(32); name.setHint(Ui.label("Profile name")); name.setValue(profileDraft); name.setResponder(value -> profileDraft = value);
        widths.add(width -> { form.setMinWidth(width-26); name.setWidth(width - 109); });
        form.addChild(name, form.newChildLayoutSettings().alignHorizontallyLeft().paddingLeft(7).paddingTop(13));
        form.addChild(new UiButton("Save", 0, 0, 55, 26, () -> {
            try {
                String valid = ProfileStore.validName(profileDraft);
                if (Neon.profiles.names().contains(valid)) throw new IllegalArgumentException("Name already exists.");
                Neon.profiles.put(valid, Neon.config); profileDraft = ""; error = ""; rebuildWidgets();
            } catch (IllegalArgumentException invalid) { error = invalid.getMessage(); }
            catch (Exception failure) { error = "Could not save profile."; Neon.LOG.error(error, failure); }
        }), form.newChildLayoutSettings().alignHorizontallyRight().paddingTop(7));
        rows.addChild(form);
    }
    private interface FileAction { void run() throws Exception; }
    private void confirm(String title, String body, FileAction action) {
        minecraft.gui.setScreen(new ConfirmSheet(this, title, body, yes -> {
            if (yes) try { action.run(); error = ""; } catch (Exception failure) { error = "Could not change profile."; Neon.LOG.error(error, failure); }
            minecraft.gui.setScreen(this);
        }));
    }
    private void refresh() { refreshers.forEach(Runnable::run); }
    private void assign(InputConstants.Key key) {
        session.change(() -> { capturing.setKey(key); capturing = null; KeyMapping.resetMapping(); minecraft.options.save(); }); rebuildWidgets();
    }
    @Override public boolean keyPressed(KeyEvent e) {
        ExactSlider edit = activeNumber();
        if (edit != null) return edit.keyPressed(e);
        if (capturing != null) {
            if (e.key() == GLFW.GLFW_KEY_ESCAPE) { capturing = null; refresh(); }
            else assign(e.key() == GLFW.GLFW_KEY_BACKSPACE || e.key() == GLFW.GLFW_KEY_DELETE ? InputConstants.UNKNOWN : InputConstants.getKey(e));
            return true;
        }
        if (e.key() == GLFW.GLFW_KEY_ESCAPE && window.dragging()) { window.cancel(); layoutWindow(); return true; }
        if (e.key() == GLFW.GLFW_KEY_ESCAPE && colorOpen) { colorOpen = revealColor = false; if (colorPicker != null) colorPicker.open(false); return true; }
        if (e.key() == GLFW.GLFW_KEY_ESCAPE && !module.isEmpty()) { back(); return true; }
        if (e.key() == GLFW.GLFW_KEY_Z && (e.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) { if (session.undo()) rebuildWidgets(); return true; }
        return super.keyPressed(e);
    }
    @Override protected boolean pixelMouseClicked(MouseButtonEvent e, boolean twice) {
        if (capturing != null) { assign(InputConstants.Type.MOUSE.getOrCreate(e.button())); return true; }
        if (e.button() == 0) {
            int corner = window.corner(e.x(), e.y());
            if (corner >= 0) { window.begin(corner, e.x(), e.y()); return true; }
        }
        if (super.pixelMouseClicked(e, twice)) return true;
        if (e.button() == 0 && e.x() >= sx && e.x() < sx + sw && e.y() >= sy && e.y() < sy + 37) {
            window.begin(4, e.x(), e.y()); return true;
        }
        return false;
    }
    @Override protected boolean pixelMouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (window.dragging()) { window.drag(e.x(), e.y()); layoutWindow(); return true; }
        return super.pixelMouseDragged(e, dx, dy);
    }
    private void layoutWindow() {
        var b = window.bounds(); int dx = b.left() - sx, dy = b.top() - sy;
        boolean resized = sw != b.width() || sh != b.height();
        sx = b.left(); sy = b.top(); sw = b.width(); sh = b.height();
        contentX = sx + side + 12; contentY = sy + 38; panelWidth = sw - side - 32;
        for (var child : children()) if (child instanceof AbstractWidget widget) { widget.setX(widget.getX() + dx); widget.setY(widget.getY() + dy); }
        if (resized && scroll != null) {
            widths.forEach(change -> change.accept(panelWidth));
            if (colorPicker != null) colorPicker.resize(controlWidth(), Math.max(30, sh - 50) - ROW_HEIGHT);
            scroll.setWidth(panelWidth + 8); scroll.setHeight(Math.max(30, sh - 50)); scroll.relayout();
        }
    }
    @Override protected boolean pixelMouseReleased(MouseButtonEvent e) {
        if (window.dragging()) { window.finish(); return true; }
        return super.pixelMouseReleased(e);
    }
    @Override protected boolean pixelMouseScrolled(double x, double y, double dx, double dy) {
        revealColor = false; return super.pixelMouseScrolled(x,y,dx,dy);
    }
    @Override protected void pixelBackground(GuiGraphicsExtractor g, int mx, int my, float delta) { Ui.background(g, canvasWidth(), canvasHeight()); }
    @Override protected void renderPixels(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (colorPicker != null) {
            if (colorPicker.tick()) scroll.relayout();
            if (revealColor) {
                scroll.scrollTo(scroll.scrollAmount() + colorPicker.top() - ROW_HEIGHT - scroll.getY());
                if (colorPicker.getHeight() >= colorPicker.getWidth() + 60) revealColor = false;
            }
            if (colorPicker.closed()) { expandedColor = ""; rebuildWidgets(); }
        }
        refresh(); Ui.window(g, sx, sy, sw, sh);
        Ui.rect(g, sx + 1, sy + 1, side - 1, sh - 2, Ui.SIDEBAR);
        Ui.rect(g, sx + side - 1, sy + 1, 1, sh - 2, UiEffects.mix(Ui.PANEL, 0xffffffff, .027));
        Ui.rect(g,sx+14,sy+119,side-28,1,0x1027313c);
        int brandX = sx + 15;
        for (char letter : "NEON".toCharArray()) { String glyph = Character.toString(letter); Ui.title(g, glyph, brandX, sy + 14); brandX += UiFont.width(glyph, 13) + 2; }
        Ui.title(g, TABS[page], contentX, sy + 14);
        g.enableScissor(contentX, contentY, contentX + panelWidth, scroll.getBottom());
        for (int i = 0; i + 1 < rowLabels.size(); i++) Ui.rect(g, contentX + 26, rowLabels.get(i).getBottom() - 1, controlWidth(), 1, 0x06ffffff);
        g.disableScissor();
        renderWidgets(g, mx, my, delta);
        if (!error.isEmpty()) Ui.small(g, Ui.fit(error, panelWidth - 105), contentX, sy + sh - 26, Ui.RED);
    }
    @Override public void onClose() {
        if (window.dragging()) { window.cancel(); layoutWindow(); return; }
        if (Neon.save()) minecraft.gui.setScreen(parent); else error = "Could not save. Check the game log.";
    }
    @Override public void removed() { rememberScroll(); capturing = null; if (window.dragging()) window.cancel(); session.end(); Neon.flushSaves(); }
}

package me.xchro.neon.ui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import java.util.function.*;
import me.xchro.neon.Neon;
import me.xchro.neon.core.NumberValue;
import me.xchro.neon.config.NeonConfig.ArmorAnchor;
import me.xchro.neon.config.NeonConfig.LeftAction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Vanilla widgets, native scrolling/focus navigation, and direct key assignment. */
public final class SettingsScreen extends Screen {
    private static final String[] TABS = {"Camera", "Autoclicker", "HUD", "Controls"};
    private final Screen parent;
    private final SettingsSession session = new SettingsSession();
    SettingsSession session() { return session; }
    private final List<Runnable> refreshers = new ArrayList<>();
    private int page, panelWidth, controlWidth;
    private LinearLayout rows;
    private KeyMapping capturing;
    private String error = "";
    public SettingsScreen(Screen parent) { super(Component.literal("Neon Settings")); this.parent = parent; }
    @Override protected void init() {
        refreshers.clear(); capturing = null;
        panelWidth = Math.min(440, width - 36); controlWidth = (panelWidth - 12) / 2;
        int tabWidth = (panelWidth - 12) / 4, left = (width - panelWidth) / 2;
        for (int i = 0; i < TABS.length; i++) {
            int tab = i;
            Button button = addRenderableWidget(Button.builder(Component.literal(TABS[i]), b -> {
                page = tab; error = ""; rebuildWidgets();
            }).bounds(left + i * (tabWidth + 4), 31, tabWidth, 20).build());
            button.active = page != i;
        }
        rows = LinearLayout.vertical().spacing(4);
        var c = Neon.config;
        switch (page) {
            case 0 -> {
                section("Freelook");
                binding("Hold to freelook", Neon.freelook, "Look around without changing aim or movement. Starts and stops immediately; restores your previous perspective.");
                section("Zoom");
                binding("Zoom key", Neon.zoom, "Works on its own or together with freelook. Choose Hold or Toggle below.");
                choice("Zoom activation", () -> c.zoomToggle ? "Toggle" : "Hold", () -> c.zoomToggle = !c.zoomToggle, "Hold: zoom while pressed. Toggle: press once to zoom and again to release. Menus and focus loss always cancel zoom.");
                slider("Magnification", c.zoomFactor, 1, 16, .5, "x", v -> c.zoomFactor = v, "4x by default. The mouse wheel can adjust this while zooming.");
                slider("Transition", c.zoomMillis, 0, 500, 10, " ms", v -> c.zoomMillis = (int) v, "Zoom entry and exit time. Set to 0 for instant zoom.");
                toggle("Scroll adjustment", () -> c.zoomWheel, v -> c.zoomWheel = v, "Use the wheel to change magnification while zoom is active. Scrolling works normally otherwise.");
                enabled(slider("Scroll step", c.zoomScrollStep, .1, 4, .1, "x", v -> c.zoomScrollStep = v, "How much each wheel step changes magnification. Click the number to enter an exact value."), () -> c.zoomWheel);
                toggle("Scale sensitivity", () -> c.zoomSensitivity, v -> c.zoomSensitivity = v, "Reduce mouse sensitivity in proportion to zoom, without changing Minecraft's saved sensitivity.");
            }
            case 1 -> {
                section("Activation");
                toggle("Autoclicker", () -> c.clickerEnabled, v -> c.clickerEnabled = v, "Allow the activation key to start clicking. Inventory and other menus always stop it. Chat and background behavior can be configured below.");
                binding("Activation key", Neon.autoclicker, "Start or stop in the game. Toggle mode can continue while typing or tabbed out. Typing this key in chat never starts or stops clicking.");
                choice("Activation mode", () -> c.toggleClicker ? "Toggle" : "Hold", () -> c.toggleClicker = !c.toggleClicker, "Hold: click while pressed. Toggle: press once to start and again to stop.");
                enabled(toggle("Continue in chat", () -> c.clickerInChat, v -> c.clickerInChat = v, "In Toggle mode, keep clicking while typing in chat. Start it before opening chat."), () -> c.toggleClicker);
                enabled(toggle("Run in background", () -> c.clickerInBackground, v -> c.clickerInBackground = v, "In Toggle mode, keep clicking while tabbed out. Prevents automatic focus-loss pausing only while the autoclicker is running."), () -> c.toggleClicker);
                section("Mouse buttons");
                toggle("Left button", () -> c.leftClick, v -> c.leftClick = v, "Enable automatic attacks or held mining, depending on Left action.");
                enabled(choice("Left action", () -> c.leftAction == LeftAction.CLICK ? "Click" : "Hold",
                    () -> c.leftAction = c.leftAction == LeftAction.CLICK ? LeftAction.HOLD : LeftAction.CLICK,
                    "Click: repeat attacks and ignore blocks entirely. Hold: keep the attack button down for continuous mining, like physically holding it. This is separate from the activation key's Hold/Toggle mode."), () -> c.leftClick);
                enabled(interval("Left interval", c.leftIntervalTicks, v -> c.leftIntervalTicks = v,
                    "0 = vanilla weapon recharge timing. 1-60 = attempt an attack every chosen number of game ticks, without waiting for full recharge. Click mode ignores blocks. Hold mode does not use this timer. At normal speed, 20 ticks = 1 second."), () -> c.leftClick && c.leftAction == LeftAction.CLICK);
                toggle("Right click", () -> c.rightClick, v -> c.rightClick = v, "Repeat normal Minecraft use input with the interval below.");
                enabled(interval("Right interval", c.rightIntervalTicks, v -> c.rightIntervalTicks = v,
                    "0 = vanilla use timing. 1-60 = attempt a use every chosen number of game ticks. Item and server interaction rules still apply. At normal speed, 20 ticks = 1 second."), () -> c.rightClick);
                section("Safeguards");
                enabled(toggle("Entities only", () -> c.clickerEntitiesOnly, v -> c.clickerEntitiesOnly = v,
                    "Only repeat attacks when the crosshair targets an entity. Prevents air swings in Click mode. Does not restrict Hold mining or right click."), () -> c.leftClick && c.leftAction == LeftAction.CLICK);
                toggle("Protect tools", () -> c.clickerProtectTools, v -> c.clickerProtectTools = v,
                    "Stop the autoclicker when a held tool or weapon reaches the durability threshold. Shows the reason in the action bar and requires fresh activation after repair or replacement.");
                enabled(slider("Stop at durability", c.clickerMinDurability, 2, 100, 1, " remaining", v -> c.clickerMinDurability = (int) v,
                    "Stop at or below this many remaining durability points. Checked for the main hand on attacks, and both hands on use."), () -> c.clickerProtectTools);
            }
            case 2 -> {
                section("Armor");
                toggle("Show armor", () -> c.armor.visible, v -> c.armor.visible = v, "Show equipped armor with native item icons, glint, and durability bars. Empty slots stay hidden.");
                choice("Layout", () -> c.armorVertical ? "Vertical" : "Horizontal", () -> c.armorVertical = !c.armorVertical, "Arrange armor in a column or a row.");
                choice("Order", () -> c.armorReverse ? "Boots first" : "Helmet first", () -> c.armorReverse = !c.armorReverse, "Reverse the order of armor items.");
                slider("Scale", c.armor.scale, .5, 3, .25, "x", v -> c.armor.scale = v, "Resize the entire armor display. It automatically fits inside the screen.");
                toggle("Slot frames", () -> c.armorSlots, v -> c.armorSlots = v, "Use Minecraft's hotbar texture, including your resource pack. Turn off for bare item icons.");
                slider("Item spacing", c.armorGap, 0, 8, 1, " px", v -> c.armorGap = (int) v, "Extra space between items. Screen edges have no forced padding.");
                choice("Position", () -> c.armorAnchor.label, () -> c.armorAnchor = ArmorAnchor.values()[(c.armorAnchor.ordinal() + 1) % ArmorAnchor.values().length], "Choose an edge or hotbar preset. Hotbar positions make room for your offhand and attack indicator. Dragging switches to Custom.");
                action("Custom placement", "Edit position...", () -> minecraft.gui.setScreen(new HudEditor(this)), "Drag to any edge, nudge with arrow keys, and use the mouse wheel to resize.");
                toggle("Durability warning", () -> c.armorWarnings, v -> c.armorWarnings = v, "Show a small amber ! on armor that needs repair. Also warns at 5 durability or less.");
                enabled(slider("Warn below", c.armorWarningPercent, 1, 30, 1, "%", v -> c.armorWarningPercent = (int) v, "Percentage of durability remaining that triggers the warning."), () -> c.armorWarnings);
                action("Restore defaults", "Reset armor", () -> { c.resetArmor(); rebuildWidgets(); }, "Reset armor layout, position, scale, and appearance.");
                section("Totems");
                toggle("Offhand total", () -> c.totems.visible, v -> c.totems.visible = v, "Show your inventory + offhand total directly on the offhand totem. No separate icon. Only appears while holding a totem in your offhand.");
            }
            case 3 -> {
                section("Click a binding to change it");
                binding("Neon settings", Neon.settings, "Open this menu from the game. All Neon bindings start unassigned to avoid conflicts.");
                binding("Freelook", Neon.freelook, "Hold to look around without changing aim.");
                binding("Zoom", Neon.zoom, "Hold or toggle zoom, depending on Camera settings.");
                binding("Autoclicker", Neon.autoclicker, "Hold or toggle, depending on your Autoclicker settings.");
                section("Esc cancels / Backspace clears");
                action("All controls", "Minecraft key binds...", () -> minecraft.gui.setScreen(new KeyBindsScreen(this, minecraft.options)), "View and resolve conflicts with other Minecraft and mod controls. Conflicting bindings are shown in red.");
            }
        }
        ScrollableLayout scroll = new ScrollableLayout(minecraft, rows, Math.max(40, height - 104));
        scroll.setMinHeight(Math.max(40, height - 104)); scroll.setScrollbarSpacing(4);
        scroll.arrangeElements(); scroll.setX((width - scroll.getWidth()) / 2); scroll.setY(59);
        scroll.visitWidgets(this::addRenderableWidget);
        int footerWidth = (panelWidth - 12) / 4;
        Button undo = addRenderableWidget(Button.builder(Component.literal("Undo"), b -> { if (session.undo()) rebuildWidgets(); }).bounds(left, height - 26, footerWidth, 20).build());
        undo.setTooltip(Tooltip.create(Component.literal("Undo the last settings change, including a complete slider drag, HUD edit, profile load, or binding change. Ctrl+Z also works.")));
        refreshers.add(() -> undo.active = session.canUndo());
        Button reset = addRenderableWidget(Button.builder(Component.literal("Reset page"), b -> { session.resetPage(page); rebuildWidgets(); }).bounds(left + footerWidth + 4, height - 26, footerWidth, 20).build());
        reset.setTooltip(Tooltip.create(Component.literal("Reset only " + TABS[page] + " to defaults. Undo restores your previous settings. Key bindings reset on the Controls page.")));
        addRenderableWidget(Button.builder(Component.literal("Profiles..."), b -> minecraft.gui.setScreen(new ProfilesScreen(this, session))).bounds(left + 2 * (footerWidth + 4), height - 26, footerWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(left + 3 * (footerWidth + 4), height - 26, footerWidth, 20).build());
        refresh();
    }
    private void section(String label) {
        rows.addChild(new StringWidget(panelWidth, 16, Component.literal(label).withStyle(ChatFormatting.GRAY), font));
    }
    private <T extends AbstractWidget> T row(String label, T control, String tip) {
        LinearLayout line = LinearLayout.horizontal().spacing(12);
        line.addChild(new StringWidget(panelWidth - controlWidth - 12, 20, Component.literal(label), font));
        control.setTooltip(Tooltip.create(Component.literal(tip)));
        line.addChild(control); rows.addChild(line); return control;
    }
    private Button action(String label, String value, Runnable action, String tip) {
        return row(label, Button.builder(Component.literal(value), b -> { session.change(action); refresh(); }).bounds(0, 0, controlWidth, 20).build(), tip);
    }
    private Button choice(String label, Supplier<String> value, Runnable action, String tip) {
        Button button = action(label, value.get(), action, tip);
        refreshers.add(() -> button.setMessage(Component.literal(value.get()))); return button;
    }
    private Button toggle(String label, BooleanSupplier value, Consumer<Boolean> set, String tip) {
        return choice(label, () -> value.getAsBoolean() ? "ON" : "OFF", () -> set.accept(!value.getAsBoolean()), tip);
    }
    private void enabled(AbstractWidget widget, BooleanSupplier enabled) { refreshers.add(() -> widget.active = enabled.getAsBoolean()); }
    private void binding(String label, KeyMapping key, String tip) {
        Button button = action(label, "", () -> capturing = key, tip + " Click, then press a key or mouse button. Esc cancels; Backspace or Delete clears.");
        refreshers.add(() -> {
            boolean conflict = !key.isUnbound() && Arrays.stream(minecraft.options.keyMappings).anyMatch(other -> other != key && key.same(other));
            Component name = key.isUnbound() ? Component.literal("Unassigned") : key.getTranslatedKeyMessage();
            button.setMessage(capturing == key ? Component.literal("> Press a key <").withStyle(ChatFormatting.YELLOW)
                : name.copy().withStyle(conflict ? ChatFormatting.RED : ChatFormatting.WHITE));
        });
    }
    private AbstractSliderButton slider(String label, double current, double min, double max, double step, String suffix, DoubleConsumer set, String tip) {
        return slider(label, current, min, max, step, n -> NumberValue.format(n) + suffix, set, tip);
    }
    private AbstractSliderButton interval(String label, int current, IntConsumer set, String tip) {
        return slider(label, current, 0, 60, 1, n -> n == 0 ? "Vanilla (0)" : (int) n + (n == 1 ? " tick" : " ticks"), v -> set.accept((int) v), tip);
    }
    private AbstractSliderButton slider(String label, double current, double min, double max, double step, DoubleFunction<String> format, DoubleConsumer set, String tip) {
        return row(label, new ExactSlider(this, session, label, controlWidth, current, min, max, step, format, set),
            tip + " Click the displayed value (or press Enter) to type. Drag the rest of the slider to adjust.");
    }
    private void refresh() { refreshers.forEach(Runnable::run); }
    private void assign(InputConstants.Key key) {
        session.change(() -> { capturing.setKey(key); capturing = null; KeyMapping.resetMapping(); minecraft.options.save(); }); refresh();
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (capturing == null) {
            if (event.key() == GLFW.GLFW_KEY_Z && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
                if (session.undo()) rebuildWidgets(); return true;
            }
            return super.keyPressed(event);
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { capturing = null; refresh(); }
        else assign(event.key() == GLFW.GLFW_KEY_BACKSPACE || event.key() == GLFW.GLFW_KEY_DELETE ? InputConstants.UNKNOWN : InputConstants.getKey(event));
        return true;
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (capturing == null) return super.mouseClicked(event, doubled);
        assign(InputConstants.Type.MOUSE.getOrCreate(event.button())); return true;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        refresh();
        super.extractRenderState(g, mx, my, delta);
        g.centeredText(font, title, width / 2, 12, 0xffffffff);
        String status = !error.isEmpty() ? error : capturing != null ? "Press a key / Esc cancels / Backspace clears" : "Hover over a setting for details";
        g.centeredText(font, status, width / 2, height - 40, error.isEmpty() ? 0xffaaaaaa : 0xffff7777);
    }
    @Override public void onClose() {
        if (Neon.save()) minecraft.gui.setScreen(parent);
        else error = "Could not save settings. Check the game log.";
    }
    @Override public void removed() { capturing = null; session.end(); Neon.save(); }
}

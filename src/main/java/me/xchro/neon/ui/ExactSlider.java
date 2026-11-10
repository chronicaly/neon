package me.xchro.neon.ui;

import java.util.function.*;
import me.xchro.neon.core.NumberValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

public final class ExactSlider extends AbstractSliderButton {
    private final Screen parent;
    private final SettingsSession session;
    private final String name;
    private final double min, max, step;
    private final DoubleFunction<String> format;
    private final DoubleConsumer set;
    private double number;
    private boolean fullRow;
    private boolean inline;
    private UiEditBox edit;
    private me.xchro.neon.core.TimedValue motion;
    private boolean draggingValue;
    private String identity;
    String identity() { return identity == null ? name : identity; }
    ExactSlider identity(String key) { identity = key; return this; }
    void inherit(ExactSlider old) { edit = old.edit; motion = old.motion; positionEdit(); }
    public boolean isEditing() { return edit != null; }
    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager sounds) {}
    public boolean finishInline(boolean apply) {
        if (edit == null) return true;
        if (apply) {
            var parsed = NumberValue.parse(edit.getValue(), min, max, step >= 1);
            if (parsed.isEmpty()) { edit.invalid(true); return false; }
            number = parsed.getAsDouble(); value = (number - min) / (max - min);
            session.change(() -> set.accept(number)); updateMessage();
        }
        edit = null; return true;
    }
    public ExactSlider fullRow() { fullRow = true; setHeight(35); return this; }
    public ExactSlider inlineRow() { fullRow = inline = true; setHeight(22); return this; }
    public ExactSlider(Screen parent, SettingsSession session, String name, int width, double current, double min, double max, double step, DoubleFunction<String> format, DoubleConsumer set) {
        super(0, 0, width, 26, Component.empty(), (current - min) / (max - min));
        this.parent = parent; this.session = session; this.name = name; this.min = min; this.max = max;
        this.step = step; this.format = format; this.set = set; number = current; updateMessage();
        motion = new me.xchro.neon.core.TimedValue(value);
    }
    private void typeValue() {
        session.end();
        edit = new UiEditBox(getRight() - 66, getY() + 4, 58, name).compact();
        edit.setMaxLength(24); edit.setValue(NumberValue.format(number)); edit.setFocused(true); edit.setHighlightPos(0);
        edit.setResponder(text -> edit.invalid(NumberValue.parse(text, min, max, step >= 1).isEmpty()));
    }
    public boolean isEditBoxOver(double x, double y) { return edit != null && x >= edit.getX() - 4 && x < edit.getRight() + 4 && y >= edit.getY() - 3 && y < edit.getY() + 18; }
    private void positionEdit() { if (edit != null) { edit.setX(getRight() - 66); edit.setY(getY() + 4); } }
    @Override public void setX(int x) { super.setX(x); positionEdit(); }
    @Override public void setY(int y) { super.setY(y); positionEdit(); }
    @Override public void setWidth(int w) { super.setWidth(w); positionEdit(); }
    @Override public void onClick(MouseButtonEvent event, boolean doubled) {
        if (edit != null) { edit.mouseClicked(event, doubled); return; }
        int textWidth = Ui.width(getMessage().getString());
        double center = fullRow ? getRight() - textWidth / 2.0 : getX() + getWidth() / 2.0;
        if (event.y() < getY() + 14 && Math.abs(event.x() - center) <= textWidth / 2.0 + 4) { typeValue(); return; }
        session.begin(); draggingValue = true; super.onClick(event, doubled); motion.snap(value);
    }
    @Override public void onRelease(MouseButtonEvent event) { super.onRelease(event); draggingValue = false; session.end(); }
    @Override protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        if (edit != null) edit.mouseDragged(event, dx, dy); else { super.onDrag(event, dx, dy); motion.snap(value); }
    }
    @Override public boolean charTyped(CharacterEvent event) { return edit != null && edit.charTyped(event); }
    @Override public boolean keyPressed(KeyEvent event) {
        if (edit != null) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) { finishInline(false); return true; }
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) { finishInline(true); return true; }
            return edit.keyPressed(event);
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) { typeValue(); return true; }
        session.begin(); try { return super.keyPressed(event); } finally { session.end(); }
    }
    @Override protected void updateMessage() { if (format != null) setMessage(Ui.label(format.apply(number))); }
    @Override public void extractWidgetRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (draggingValue) motion.snap(value);
        double displayed = motion.to(value, System.nanoTime(), 100_000_000L);
        if (inline) {
            Ui.rect(g, getX(), getY(), getWidth(), getHeight(), Ui.FIELD);
            Ui.rect(g, getX(), getY(), (int) Math.round(getWidth() * displayed), getHeight(), Ui.alpha(Ui.BLUE, 16));
            Ui.small(g, name, getX() + 7, getY() + 6, Ui.TEXT);
            if (edit == null) Ui.small(g, getMessage().getString(), getRight() - UiFont.width(getMessage().getString(), 10) - 7, getY() + 6, active ? Ui.BLUE : Ui.MUTED);
            else { edit.setX(getRight() - 66); edit.setY(getY() + 4); edit.extractRenderState(g, mx, my, delta); }
            if (isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard()) Ui.stroke(g, getX(), getY(), getWidth(), getHeight(), Ui.BLUE);
            return;
        }
        if (isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard()) Ui.stroke(g, getX(), getY(), getWidth(), getHeight(), Ui.BLUE);
        if (fullRow) {
            Ui.text(g, name, getX(), getY() + 1, Ui.TEXT);
            if (edit == null) Ui.text(g, getMessage().getString(), getRight() - Ui.width(getMessage().getString()), getY() + 1, active ? Ui.BLUE : Ui.MUTED);
        } else if (edit == null) Ui.centered(g, getMessage().getString(), getX() + getWidth() / 2, getY() + 1, active ? Ui.TEXT : Ui.MUTED);
        if (edit != null) edit.extractRenderState(g, mx, my, delta);
        int x = getX() + (fullRow ? 3 : 6), y = getY() + 23, track = getWidth() - (fullRow ? 6 : 12);
        Ui.rect(g, x, y, track, 2, 0xff354451);
        Ui.rect(g, x, y, (int) Math.round(track * displayed), 2, active ? Ui.BLUE : Ui.MUTED);
        int knob = x + (int) Math.round(track * displayed);
        if (active) Ui.glow(g, knob - 3, y - 2, 6, 6, Ui.BLUE, 3);
        Ui.rect(g, knob - 3, y - 2, 6, 6, active ? Ui.TEXT : Ui.MUTED);
    }
    @Override protected MutableComponent createNarrationMessage() {
        return Component.literal(name + ": ").append(super.createNarrationMessage());
    }
    @Override protected void applyValue() {
        number = Math.clamp(Math.round((min + value * (max - min)) / step) * step, min, max);
        number = Double.parseDouble(NumberValue.format(number));
        set.accept(number); updateMessage();
    }
}

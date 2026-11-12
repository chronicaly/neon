package me.xchro.neon.ui;

import java.util.function.*;
import me.xchro.neon.core.NumberValue;
import me.xchro.neon.core.NumberDrag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

public final class ExactSlider extends AbstractSliderButton {
    private static final long VALUE_MOTION_NANOS = 120_000_000L;
    private final Screen parent;
    private final SettingsSession session;
    private final String name;
    private final double min, max, step;
    private final DoubleFunction<String> format;
    private final DoubleConsumer set;
    private double number;
    private boolean fullRow;
    private boolean inline;
    private boolean compact;
    private int compactValueWidth;
    private UiEditBox edit;
    private me.xchro.neon.core.UiMotion motion;
    private boolean draggingValue;
    private NumberDrag drag;
    private String identity;
    String identity() { return identity == null ? name : identity; }
    ExactSlider identity(String key) { identity = key; return this; }
    void inherit(ExactSlider old) { edit = old.edit; motion = old.motion; positionEdit(); }
    public boolean isEditing() { return edit != null; }
    boolean isValueDragging() { return draggingValue; }
    void finishDrag(boolean cancel) {
        if (!draggingValue) return;
        draggingValue = false; drag = null;
        if (cancel) session.cancel(); else session.end();
    }
    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager sounds) {}
    public boolean finishInline(boolean apply) {
        if (edit == null) return true;
        if (apply) {
            var parsed = NumberValue.parse(edit.getValue(), min, max, step >= 1);
            if (parsed.isEmpty()) { edit.invalid(true); return false; }
            boolean changed = parsed.getAsDouble() != number;
            number = parsed.getAsDouble(); value = (number - min) / (max - min);
            motion.to(value, System.nanoTime(), VALUE_MOTION_NANOS);
            session.change(() -> set.accept(number)); updateMessage();
            if (changed) UiSounds.cue(me.xchro.neon.core.UiSoundPolicy.Cue.CONFIRM);
        }
        edit = null;
        if (compact && apply) {
            compactValueWidth = compactValueWidth(); compactLayout();
            if (parent instanceof SettingsScreen menu) menu.relayoutControls();
        }
        return true;
    }
    public ExactSlider fullRow() { fullRow = true; setHeight(35); return this; }
    public ExactSlider inlineRow() { fullRow = inline = true; setHeight(22); return this; }
    public ExactSlider compactRow() {
        compact = true;
        compactValueWidth = compactValueWidth();
        compactLayout(); return this;
    }
    private int compactValueWidth() {
        return Math.max(36, Math.max(Ui.width(format.apply(min)), Math.max(Ui.width(format.apply(max)), Ui.width(format.apply(number)))) + 6);
    }
    private int compactInlineLeft() {
        return getX() + Math.max(Ui.width(name) + 12, Math.min(112, getWidth() / 2 - 10));
    }
    private boolean compactStacked() { return getRight() - compactValueWidth - 9 - compactInlineLeft() < 90; }
    private void compactLayout() { if (compact) setHeight(compactStacked() ? 44 : 27); }
    private int compactTrackRight() { return compactStacked() ? getRight() - 3 : getRight() - compactValueWidth - 9; }
    private int compactTrackLeft() {
        return compactStacked() ? getX() + 3 : Math.max(compactInlineLeft(), compactTrackRight() - 150);
    }
    private int compactTrackY() { return getY() + (compactStacked() ? 31 : 13); }
    private double trackLeft() { return inline ? getX() : compact ? compactTrackLeft() : getX() + (fullRow ? 3 : 6); }
    private double trackWidth() { return Math.max(1, inline ? getWidth() : compact ? compactTrackRight() - compactTrackLeft() : getWidth() - (fullRow ? 6 : 12)); }
    private boolean exactTarget(double x,double y) {
        if(inline)return false;
        if(compact)return x>=getRight()-compactValueWidth&&(!compactStacked()||y<getY()+22);
        int textWidth=Ui.width(getMessage().getString());double center=fullRow?getRight()-textWidth/2.0:getX()+getWidth()/2.0;
        return y<getY()+14&&Math.abs(x-center)<=textWidth/2.0+4;
    }
    private void accept(double next) {
        number = next; value = (number - min) / (max - min);
        set.accept(number); updateMessage();
        // Apply the exact value immediately; only the painted fill trails briefly.
        motion.to(value, System.nanoTime(), VALUE_MOTION_NANOS);
    }
    private void beginDrag(MouseButtonEvent event) {
        session.begin(); draggingValue = true;
        boolean fine = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
        drag = new NumberDrag(min, max, step, number, event.x(), trackLeft(), trackWidth(), fine);
        if (!fine) accept(drag.value());
        if (parent instanceof PixelScreen screen) screen.capturePointer(this, event);
    }
    public ExactSlider(Screen parent, SettingsSession session, String name, int width, double current, double min, double max, double step, DoubleFunction<String> format, DoubleConsumer set) {
        super(0, 0, width, 26, Component.empty(), (current - min) / (max - min));
        this.parent = parent; this.session = session; this.name = name; this.min = min; this.max = max;
        this.step = step; this.format = format; this.set = set; number = current; updateMessage();
        motion = new me.xchro.neon.core.UiMotion(value);
    }
    private void typeValue() {
        session.end();
        edit = new UiEditBox(getRight() - 66, getY() + 4, 58, name).compact();
        positionEdit();
        edit.setMaxLength(24); edit.setValue(NumberValue.format(number)); edit.setFocused(true); edit.setHighlightPos(0);
        edit.setResponder(text -> edit.invalid(NumberValue.parse(text, min, max, step >= 1).isEmpty()));
    }
    public boolean isEditBoxOver(double x, double y) { return edit != null && x >= edit.getX() - 4 && x < edit.getRight() + 4 && y >= edit.getY() - 3 && y < edit.getY() + 18; }
    private void positionEdit() {
        if (edit != null) {
            int editWidth = compact ? Math.max(50, compactValueWidth - 8) : 58;
            edit.setX(inline ? getX() + 10 : compact ? getRight() - editWidth - 4 : getRight() - 66);
            edit.setY(getY() + (compact ? 7 : 4));
            edit.setWidth(inline ? getWidth() - 20 : editWidth);
        }
    }
    @Override public void setX(int x) { super.setX(x); positionEdit(); }
    @Override public void setY(int y) { super.setY(y); positionEdit(); }
    @Override public void setWidth(int w) { super.setWidth(w); compactLayout(); positionEdit(); }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        // Unity's drawRange: right click anywhere in the row for exact input.
        if ((inline || compact) && active && event.button() == 1 && isMouseOver(event.x(), event.y())) {
            if (edit == null) typeValue();
            return true;
        }
        return super.mouseClicked(event, doubled);
    }
    @Override public void onClick(MouseButtonEvent event, boolean doubled) {
        if (edit != null) { edit.mouseClicked(event, doubled); return; }
        if (inline) { beginDrag(event); return; }
        if (compact) {
            if (exactTarget(event.x(),event.y())) { typeValue(); return; }
            if (event.x() < compactTrackLeft() - 4 || compactStacked() && event.y() < getY() + 22) return;
            beginDrag(event); return;
        }
        if (exactTarget(event.x(),event.y())) { typeValue(); return; }
        beginDrag(event);
    }
    @Override public void onRelease(MouseButtonEvent event) { finishDrag(false); }
    @Override protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        if (edit != null) edit.mouseDragged(event, dx, dy);
        else if (draggingValue && dx != 0) accept(drag.move(event.x(), trackLeft(), trackWidth(), (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0));
    }
    @Override public boolean charTyped(CharacterEvent event) { return edit != null && edit.charTyped(event); }
    @Override public boolean keyPressed(KeyEvent event) {
        if (!active) return false;
        if (edit != null) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) { finishInline(false); return true; }
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) { finishInline(true); return true; }
            return edit.keyPressed(event);
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) { typeValue(); return true; }
        if (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_RIGHT) {
            int direction = event.key() == GLFW.GLFW_KEY_LEFT ? -1 : 1;
            session.change(() -> accept(NumberDrag.nudge(number, min, max, step, direction, (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0)));
            return true;
        }
        return false;
    }
    @Override protected void updateMessage() { if (format != null) setMessage(Ui.label(format.apply(number))); }
    @Override public void extractWidgetRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g, int mx, int my, float delta) {
        if(active&&isHovered()&&edit==null){
            if(exactTarget(mx,my))g.requestCursor(com.mojang.blaze3d.platform.cursor.CursorTypes.IBEAM);
            else if(inline||mx>=trackLeft()&&mx<=trackLeft()+trackWidth()&&(!compact||!compactStacked()||my>=getY()+22))g.requestCursor(com.mojang.blaze3d.platform.cursor.CursorTypes.RESIZE_EW);
        }
        double displayed = motion.to(value, System.nanoTime(), VALUE_MOTION_NANOS);
        if (inline) {
            Ui.rect(g, getX(), getY(), getWidth(), getHeight(), HudEditor.surface(Ui.PANEL, 100));
            fill(g, getX(), getY(), (float) (getWidth() * displayed), getHeight(), HudEditor.surface(Ui.BLUE, 32));
            if (edit == null) Ui.text(g, Ui.fit(name + ": " + getMessage().getString(), getWidth() - 16), getX() + 10, getY() + 6, active ? Ui.TEXT : Ui.MUTED);
            else { positionEdit(); edit.extractRenderState(g, mx, my, delta); }
            if (isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard()) Ui.stroke(g, getX(), getY(), getWidth(), getHeight(), Ui.BLUE);
            return;
        }
        if (isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard()) Ui.stroke(g, getX(), getY(), getWidth(), getHeight(), Ui.BLUE);
        if (compact) {
            int y = compactTrackY(), x = compactTrackLeft(), track = compactTrackRight() - x;
            Ui.text(g, name, getX(), getY() + 8, Ui.TEXT);
            if (edit == null) Ui.text(g, getMessage().getString(), getRight() - Ui.width(getMessage().getString()), getY() + 8, active ? Ui.BLUE : Ui.MUTED);
            drawTrack(g, x, y, track, displayed);
            if (edit != null) { positionEdit(); edit.extractRenderState(g, mx, my, delta); }
            return;
        }
        if (fullRow) {
            Ui.text(g, name, getX(), getY() + 1, Ui.TEXT);
            if (edit == null) Ui.text(g, getMessage().getString(), getRight() - Ui.width(getMessage().getString()), getY() + 1, active ? Ui.BLUE : Ui.MUTED);
        } else if (edit == null) Ui.centered(g, getMessage().getString(), getX() + getWidth() / 2, getY() + 1, active ? Ui.TEXT : Ui.MUTED);
        if (edit != null) edit.extractRenderState(g, mx, my, delta);
        int x = getX() + (fullRow ? 3 : 6), y = getY() + Math.min(23, getHeight() - 4), track = getWidth() - (fullRow ? 6 : 12);
        drawTrack(g, x, y, track, displayed);
    }
    private void drawTrack(net.minecraft.client.gui.GuiGraphicsExtractor g, int x, int y, int width, double displayed) {
        if (width <= 0) return;
        int accent = active ? Ui.BLUE : Ui.MUTED;
        fill(g, x, y - 4, width, 8, Ui.LINE);
        fill(g, x, y - 4, (float) (Math.clamp(displayed, 0, 1) * width), 8, accent);
    }
    private static void fill(net.minecraft.client.gui.GuiGraphicsExtractor g, float x, float y, float width, float height, int color) {
        // Keep fractional positions through rendering instead of stepping whole UI units.
        UiEffects.gradient(g, x, y, width, height, color, color, color, color);
    }
    @Override protected MutableComponent createNarrationMessage() {
        return Component.literal(name + ": ").append(super.createNarrationMessage());
    }
    @Override protected void applyValue() {
        accept(NumberDrag.quantize(min + value * (max - min), min, max, step));
    }
}

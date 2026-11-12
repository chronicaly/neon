package me.xchro.neon.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.EditBox;
import org.lwjgl.glfw.GLFW;
import net.minecraft.network.chat.Component;
import me.xchro.neon.core.UiSoundPolicy.Cue;

/** Fixed display density, independent of Minecraft's gameplay HUD scale. */
public abstract class PixelScreen extends Screen {
    public static final float UI_SCALE = 1.3225f;
    private long opened;
    private boolean soundOpened;
    private double soundX = Double.NaN, soundY = Double.NaN;
    private GuiEventListener pointerOwner;
    private MouseButtonEvent pointerEvent;
    private final java.util.Map<String, me.xchro.neon.core.PressFeedback> presses = new java.util.HashMap<>();
    private record HeldPress(String identity, me.xchro.neon.core.PressFeedback state, int mouse, int key) {}
    private final java.util.List<HeldPress> heldPresses = new java.util.ArrayList<>();
    private final java.util.Map<String,me.xchro.neon.core.ActionFeedback> acknowledgements=new java.util.HashMap<>();
    private final java.util.Map<String,Long> pendingActions=new java.util.HashMap<>();
    private long actionSequence;
    final me.xchro.neon.core.ActionFeedback acknowledgement(String identity) {
        var feedback=acknowledgements.get(identity);
        if(feedback!=null&&feedback.amount(System.nanoTime())==0){acknowledgements.remove(identity);return null;}
        return feedback;
    }
    final void acknowledge(String identity,String label){acknowledgements.put(identity,new me.xchro.neon.core.ActionFeedback(label,System.nanoTime()));}
    protected final long actionToken(String identity){long token=++actionSequence;pendingActions.put(identity,token);return token;}
    protected final boolean currentAction(String identity,long token){return minecraft.gui.screen()==this&&java.util.Objects.equals(pendingActions.get(identity),token);}
    protected final void clearAcknowledgements(){acknowledgements.clear();pendingActions.clear();}
    final double pressAmount(String identity) {
        var state = presses.get(identity); return state == null ? 0 : state.value(System.nanoTime());
    }
    final void press(String identity, net.minecraft.client.input.InputWithModifiers event) {
        var state = presses.computeIfAbsent(identity, ignored -> new me.xchro.neon.core.PressFeedback());
        long now = System.nanoTime(); state.press(now);
        heldPresses.removeIf(held -> held.state == state);
        if (event instanceof MouseButtonEvent mouse) heldPresses.add(new HeldPress(identity, state, mouse.button(), -1));
        else if (event instanceof net.minecraft.client.input.KeyEvent key) heldPresses.add(new HeldPress(identity, state, -1, key.key()));
        else state.release(now);
    }
    private void releasePresses(int mouse, int key) {
        long now = System.nanoTime();
        heldPresses.removeIf(held -> { if (mouse >= 0 && held.mouse == mouse || key >= 0 && held.key == key) { held.state.release(now); return true; } return false; });
    }
    private void clearPresses() { presses.values().forEach(me.xchro.neon.core.PressFeedback::clear); heldPresses.clear(); }
    private static boolean containsControl(ContainerEventHandler parent,String identity) {
        for(var child:parent.children()) {
            if(child instanceof UiButton button&&button.identity().equals(identity))return true;
            if(child instanceof ContainerEventHandler group&&containsControl(group,identity))return true;
        }
        return false;
    }
    @Override protected void rebuildWidgets() {
        super.rebuildWidgets();
        clearRemovedPresses();
    }
    @Override protected void removeWidget(GuiEventListener widget) { super.removeWidget(widget);clearRemovedPresses(); }
    private void clearRemovedPresses() {
        heldPresses.removeIf(held->{if(containsControl(this,held.identity))return false;held.state.clear();presses.remove(held.identity);return true;});
    }
    final void capturePointer(GuiEventListener owner, MouseButtonEvent event) {
        pointerOwner = owner; pointerEvent = event;
        freezeScroll(this);
    }
    private static void freezeScroll(ContainerEventHandler group) {
        if (group instanceof UiScroll scroll) scroll.stopMotion();
        for (var child : group.children()) if (child instanceof ContainerEventHandler nested) freezeScroll(nested);
    }
    private static void clearDragging(ContainerEventHandler group) {
        group.setDragging(false);
        for (var child : group.children()) if (child instanceof ContainerEventHandler nested) clearDragging(nested);
    }
    protected boolean hasPointerGesture() { return pointerOwner != null; }
    protected void finishPointerGesture() {
        if (pointerOwner != null) {
            var owner = pointerOwner; pointerOwner = null;
            owner.mouseReleased(pointerEvent);
        }
        clearDragging(this);
    }
    protected final boolean cancelNumberDrag(net.minecraft.client.input.KeyEvent event) {
        if (event.key() != GLFW.GLFW_KEY_ESCAPE || !(pointerOwner instanceof ExactSlider slider)) return false;
        pointerOwner = null; slider.finishDrag(true); clearDragging(this); rebuildWidgets(); return true;
    }
    @Override public void tick() {
        super.tick();
        if (!minecraft.isWindowActive() && hasPointerGesture()) finishPointerGesture();
        if (!minecraft.isWindowActive()) clearPresses();
    }
    @Override public void removed() { finishPointerGesture(); clearPresses(); presses.clear(); clearAcknowledgements(); }
    @Override public void added() {
        super.added();
        MenuPresentation.clearExit();
        soundX = soundY = Double.NaN;
        if (!soundOpened) { soundOpened = true; UiSounds.cue(Cue.NAVIGATE); }
    }
    protected boolean soundControlsVisible() { return !hasPointerGesture(); }
    private String soundTarget(double x, double y) { return soundControlsVisible() ? UiSoundTargets.pointer(this, x, y) : null; }
    private float fade() { long now = System.nanoTime(); if (opened == 0) opened = now; double t = Math.clamp((now - opened) / 120_000_000.0, 0, 1); return (float) (1 - Math.pow(1 - t, 3)); }
    protected PixelScreen(Component title) { super(title); }
    public final int pixelScale() { return Math.max(1, minecraft.getWindow().getGuiScale()); }
    public final int canvasWidth() { return (int) (Math.min(width * pixelScale(), minecraft.getWindow().getWidth()) / UI_SCALE); }
    public final int canvasHeight() { return (int) (Math.min(height * pixelScale(), minecraft.getWindow().getHeight()) / UI_SCALE); }
    public final double guiCoordinate(double canvas) { return canvas * UI_SCALE / pixelScale(); }
    public final double canvasCoordinate(double gui) { return gui * pixelScale() / UI_SCALE; }
    private MouseButtonEvent pixels(MouseButtonEvent event) {
        return new MouseButtonEvent(canvasCoordinate(event.x()), canvasCoordinate(event.y()), event.buttonInfo());
    }
    @Override public final void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Ui.opacity = fade();
        g.pose().pushMatrix(); g.pose().scale(UI_SCALE / pixelScale());
        try { pixelBackground(g, (int) canvasCoordinate(mx), (int) canvasCoordinate(my), delta); }
        finally { Ui.opacity = 1; g.pose().popMatrix(); }
    }
    protected void pixelBackground(GuiGraphicsExtractor g, int mx, int my, float delta) { Ui.background(g, canvasWidth(), canvasHeight()); }
    @Override public final void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Ui.refreshTheme(); UiFont.beginFrame();
        Ui.opacity = fade();
        g.pose().pushMatrix(); g.pose().scale(UI_SCALE / pixelScale());
        try { renderPixels(g, (int) canvasCoordinate(mx), (int) canvasCoordinate(my), delta); cursor(g,(int)canvasCoordinate(mx),(int)canvasCoordinate(my)); }
        finally { Ui.opacity = 1; UiFont.flush(); g.pose().popMatrix(); }
    }
    protected abstract void renderPixels(GuiGraphicsExtractor g, int mx, int my, float delta);
    protected void cursor(GuiGraphicsExtractor g,int mx,int my) {
        if(pointerOwner instanceof ExactSlider)g.requestCursor(com.mojang.blaze3d.platform.cursor.CursorTypes.RESIZE_EW);
        else if(pointerOwner instanceof EditBox)g.requestCursor(com.mojang.blaze3d.platform.cursor.CursorTypes.IBEAM);
    }
    protected final void renderWidgets(GuiGraphicsExtractor g, int mx, int my, float delta) { super.extractRenderState(g, mx, my, delta); }
    protected final ExactSlider activeNumber() { return activeNumber(this); }
    @Override public final boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        UiSounds.beginInput();
        String before = UiSoundTargets.focus(this);
        try {
            boolean handled = pixelKeyPressed(event);
            if (minecraft.gui.screen() == this && minecraft.isWindowActive() && soundControlsVisible()
                && minecraft.getLastInputType().isKeyboard()) {
                String next = UiSoundTargets.focus(this);
                if (!java.util.Objects.equals(before, next)) UiSounds.hover(next);
            }
            return handled;
        } finally { UiSounds.endInput(); }
    }
    protected boolean pixelKeyPressed(net.minecraft.client.input.KeyEvent event) {
        boolean handled = super.keyPressed(event);
        if (minecraft.getLastInputType().isKeyboard()) revealFocus(this);
        return handled;
    }
    private static void revealFocus(net.minecraft.client.gui.components.events.ContainerEventHandler parent) {
        var target = parent.getFocused();
        if (target instanceof net.minecraft.client.gui.components.events.ContainerEventHandler group) revealFocus(group);
        while (target instanceof net.minecraft.client.gui.components.events.ContainerEventHandler group && group.getFocused() != null) target = group.getFocused();
        if (parent instanceof UiScroll scroll && target instanceof net.minecraft.client.gui.components.AbstractWidget widget) scroll.reveal(widget.getY(),widget.getBottom());
    }
    @Override public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        var number = activeNumber(); return number != null ? number.charTyped(event) : super.charTyped(event);
    }
    @Override public boolean keyReleased(net.minecraft.client.input.KeyEvent event) {
        releasePresses(-1, event.key()); return super.keyReleased(event);
    }
    private static ExactSlider activeNumber(net.minecraft.client.gui.components.events.ContainerEventHandler parent) {
        for (var child : parent.children()) {
            if (child instanceof ExactSlider slider && slider.isEditing()) return slider;
            if (child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler group) {
                ExactSlider result = activeNumber(group); if (result != null) return result;
            }
        }
        return null;
    }
    @Override public final boolean mouseClicked(MouseButtonEvent e, boolean twice) {
        UiSounds.beginInput();
        try { return inputMouseClicked(e, twice); }
        finally { UiSounds.endInput(); }
    }
    private boolean inputMouseClicked(MouseButtonEvent e, boolean twice) {
        if (hasPointerGesture()) return true;
        var p = pixels(e); var edit = activeNumber();
        if (edit != null && !edit.isEditBoxOver(p.x(), p.y()) && !edit.finishInline(true)) return true;
        boolean handled = pixelMouseClicked(p, twice);
        if (handled && p.button() == 0 && pointerOwner == null) {
            var focused = getFocused();
            while (focused instanceof ContainerEventHandler group && group.getFocused() != null) focused = group.getFocused();
            if (focused instanceof EditBox) capturePointer(focused, p);
        }
        if (hasPointerGesture()) freezeScroll(this);
        return handled;
    }
    @Override public final boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        var p = pixels(e);
        if (pointerOwner != null) {
            if (p.button() == pointerEvent.button()) { pointerEvent = p; pointerOwner.mouseDragged(p, canvasCoordinate(dx), canvasCoordinate(dy)); }
            return true;
        }
        return pixelMouseDragged(p, canvasCoordinate(dx), canvasCoordinate(dy));
    }
    @Override public final boolean mouseReleased(MouseButtonEvent e) {
        releasePresses(e.button(), -1);
        if (pointerOwner != null) {
            if (e.button() == pointerEvent.button()) { pointerEvent = pixels(e); finishPointerGesture(); }
            return true;
        }
        return pixelMouseReleased(pixels(e));
    }
    @Override public final boolean mouseScrolled(double x, double y, double dx, double dy) {
        return hasPointerGesture() || pixelMouseScrolled(canvasCoordinate(x), canvasCoordinate(y), dx, dy);
    }
    @Override public final void mouseMoved(double x, double y) {
        double px = canvasCoordinate(x), py = canvasCoordinate(y);
        super.mouseMoved(px, py);
        String target = soundTarget(px, py);
        if (Double.isNaN(soundX) || !minecraft.isWindowActive() || !soundControlsVisible()) UiSounds.rememberHover(target);
        else if (px != soundX || py != soundY) UiSounds.hover(target);
        soundX = px; soundY = py;
    }
    protected boolean pixelMouseClicked(MouseButtonEvent e, boolean twice) { return super.mouseClicked(e, twice); }
    protected boolean pixelMouseDragged(MouseButtonEvent e, double dx, double dy) { return super.mouseDragged(e, dx, dy); }
    protected boolean pixelMouseReleased(MouseButtonEvent e) { return super.mouseReleased(e); }
    protected boolean pixelMouseScrolled(double x, double y, double dx, double dy) { return super.mouseScrolled(x, y, dx, dy); }
}

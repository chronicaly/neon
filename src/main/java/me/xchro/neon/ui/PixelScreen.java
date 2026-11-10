package me.xchro.neon.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Fixed display density, independent of Minecraft's gameplay HUD scale. */
public abstract class PixelScreen extends Screen {
    public static final float UI_SCALE = 1.3225f;
    private long opened;
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
        try { renderPixels(g, (int) canvasCoordinate(mx), (int) canvasCoordinate(my), delta); }
        finally { Ui.opacity = 1; UiFont.flush(); g.pose().popMatrix(); }
    }
    protected abstract void renderPixels(GuiGraphicsExtractor g, int mx, int my, float delta);
    protected final void renderWidgets(GuiGraphicsExtractor g, int mx, int my, float delta) { super.extractRenderState(g, mx, my, delta); }
    protected final ExactSlider activeNumber() { return activeNumber(this); }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
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
        var p = pixels(e); var edit = activeNumber();
        if (edit != null && !edit.isEditBoxOver(p.x(), p.y()) && !edit.finishInline(true)) return true;
        return pixelMouseClicked(p, twice);
    }
    @Override public final boolean mouseDragged(MouseButtonEvent e, double dx, double dy) { return pixelMouseDragged(pixels(e), canvasCoordinate(dx), canvasCoordinate(dy)); }
    @Override public final boolean mouseReleased(MouseButtonEvent e) { return pixelMouseReleased(pixels(e)); }
    @Override public final boolean mouseScrolled(double x, double y, double dx, double dy) { return pixelMouseScrolled(canvasCoordinate(x), canvasCoordinate(y), dx, dy); }
    @Override public final void mouseMoved(double x, double y) { super.mouseMoved(canvasCoordinate(x), canvasCoordinate(y)); }
    protected boolean pixelMouseClicked(MouseButtonEvent e, boolean twice) { return super.mouseClicked(e, twice); }
    protected boolean pixelMouseDragged(MouseButtonEvent e, double dx, double dy) { return super.mouseDragged(e, dx, dy); }
    protected boolean pixelMouseReleased(MouseButtonEvent e) { return super.mouseReleased(e); }
    protected boolean pixelMouseScrolled(double x, double y, double dx, double dy) { return super.mouseScrolled(x, y, dx, dy); }
}

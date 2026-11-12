package me.xchro.neon.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import me.xchro.neon.mixin.EditBoxAccessor;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

class UiEditBox extends EditBox {
    private int viewStart, viewEnd;
    private boolean compact;
    private boolean invalid;
    void invalid(boolean value) { invalid = value; }
    void inherit(UiEditBox old) {
        // Live valid values belong to the new model after Undo/profile load. Only a focused
        // matching value or unfinished invalid draft should survive a structural reflow.
        if (!old.isFocused() || !old.invalid && !getValue().equals(old.getValue())) return;
        setValue(old.getValue()); setCursorPosition(old.getCursorPosition());
        setHighlightPos(((EditBoxAccessor) (Object) old).neon$highlight());
        viewStart = old.viewStart; viewEnd = old.viewEnd; invalid = old.invalid;
    }
    UiEditBox compact() { compact = true; return this; }
    UiEditBox(int x, int y, int width, String name) {
        super(Minecraft.getInstance().font, x, y, width, 24, Ui.label(name));
        setBordered(false); setTextColor(Ui.TEXT); setTextColorUneditable(Ui.MUTED); setTextShadow(false);
        addFormatter((text, start) -> Ui.label(text).getVisualOrderText());
    }
    @Override public void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (!isVisible()) return;
        updateViewport();
        var state = (EditBoxAccessor) (Object) this;
        int inset = compact ? 4 : 7, top = compact ? 3 : 6, height = compact ? 21 : 26;
        Ui.rect(g, getX() - inset, getY() - top, getWidth() + inset * 2, height, invalid ? Ui.RED : isFocused() ? Ui.BLUE : Ui.LINE);
        Ui.rect(g, getX() - inset + 1, getY() - top + 1, getWidth() + inset * 2 - 2, height - 2, Ui.FIELD);
        String value = getValue();
        int cursor = Math.clamp(getCursorPosition(), viewStart, viewEnd), anchor = Math.clamp(state.neon$highlight(), viewStart, viewEnd);
        int caretX = getX() + Ui.width(value.substring(viewStart, cursor));
        Ui.clip(g, getX() - 1, getY() - 3, getRight() + 2, getY() + 18);
        if (cursor != anchor) {
            int anchorX = getX() + Ui.width(value.substring(viewStart, anchor));
            Ui.rect(g, Math.min(caretX, anchorX), getY(), Math.abs(caretX - anchorX), 14, Ui.alpha(Ui.BLUE, 65));
        }
        if (value.isEmpty() && !isFocused() && state.neon$hint() != null)
            Ui.text(g, Ui.fit(state.neon$hint().getString(), getWidth()), getX(), getY() + 1, Ui.MUTED);
        else Ui.text(g, value.substring(viewStart, viewEnd), getX(), getY() + 1, state.neon$editable() ? Ui.TEXT : Ui.MUTED);
        if (isFocused() && (Util.getMillis() - state.neon$focusedTime()) / 500 % 2 == 0)
            Ui.rect(g, caretX, getY(), 1, 14, Ui.BLUE);
        g.disableScissor();
        if (isHovered()) g.requestCursor(state.neon$editable() ? CursorTypes.IBEAM : CursorTypes.NOT_ALLOWED);
        if (state.neon$preedit() != null) {
            var screen = (PixelScreen) Minecraft.getInstance().gui.screen();
            state.neon$preedit().updateInputPosition((int) screen.guiCoordinate(caretX), (int) screen.guiCoordinate(getY()));
            g.setPreeditOverlay(state.neon$preedit());
        }
    }
    private void updateViewport() {
        String value = getValue(); int cursor = getCursorPosition(), available = Math.max(1, getWidth() - 2);
        viewStart = Math.clamp(viewStart, 0, cursor);
        while (viewStart < cursor && Ui.width(value.substring(viewStart, cursor)) > available)
            viewStart = value.offsetByCodePoints(viewStart, 1);
        while (viewStart > 0) {
            int previous = value.offsetByCodePoints(viewStart, -1);
            if (Ui.width(value.substring(previous, cursor)) > available) break;
            viewStart = previous;
        }
        viewEnd = viewStart;
        while (viewEnd < value.length()) {
            int next = value.offsetByCodePoints(viewEnd, 1);
            if (Ui.width(value.substring(viewStart, next)) > available) break;
            viewEnd = next;
        }
    }
    private int hit(double x) {
        updateViewport(); String value = getValue();
        for (int i = viewStart; i < viewEnd;) {
            int next = value.offsetByCodePoints(i, 1);
            double middle = (Ui.width(value.substring(viewStart, i)) + Ui.width(value.substring(viewStart, next))) / 2.0;
            if (x - getX() < middle) return i;
            i = next;
        }
        return viewEnd;
    }
    @Override public void onClick(MouseButtonEvent event, boolean twice) {
        int position = hit(event.x());
        moveCursorTo(position, (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0);
        if (twice) {
            String value = getValue(); int left = position, right = position;
            while (left > 0 && !Character.isWhitespace(value.codePointBefore(left))) left = value.offsetByCodePoints(left, -1);
            while (right < value.length() && !Character.isWhitespace(value.codePointAt(right))) right = value.offsetByCodePoints(right, 1);
            moveCursorTo(left, false); moveCursorTo(right, true);
        }
    }
    @Override protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        int position = hit(event.x()); String value = getValue();
        if (event.x() < getX() && position > 0) position = value.offsetByCodePoints(position, -1);
        if (event.x() > getRight() && position < value.length()) position = value.offsetByCodePoints(position, 1);
        moveCursorTo(position, true);
    }
}

package me.xchro.neon.ui;

import java.awt.Color;
import java.util.*;
import java.util.function.*;
import me.xchro.neon.Neon;
import me.xchro.neon.core.UiSoundPolicy.Cue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.*;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Color drawer contents; the parent owns the shared surface, title, outline and reveal. */
final class ColorPicker extends AbstractContainerWidget {
    private static final String PASTE_HELP = "Paste #RGB, #RRGGBB, #RRGGBBAA, R,G,B[,A], hsb:H,S,B, or a Minecraft color name.";
    private final SettingsSession session;
    private final int initial;
    private IntSupplier synced;
    private BiConsumer<Integer, Boolean> changed;
    private int lastApplied;
    private BooleanSupplier linked;
    private Runnable toggleLink;
    private boolean themeAlpha = true;
    private float hue, saturation, brightness, alpha;
    private final Picker shade = new Picker("Saturation and brightness", 1), hueBar = new Picker("Hue", 2), alphaBar = new Picker("Opacity", 3);
    private final UiButton copy, paste, sync;
    private final UiEditBox hex;
    private final Preview originalPreview, currentPreview;
    private final List<AbstractWidget> controls;
    private boolean invalidPaste, syncingHex;
    private Integer hexStart;
    private boolean validHex;
    private int square = 144, visibleWidth;
    ColorPicker(String name, int color, SettingsSession session, IntSupplier synced, BiConsumer<Integer, Boolean> changed) {
        super(0, 0, 144, 230, Ui.label("Color picker"));
        this.initial = color; this.lastApplied = color; this.session = session; this.synced = synced; this.changed = changed; set(color);
        copy = new UiButton("Copy", 0, 0, 78, 21, this::copy).identity("color/"+name+"/copy").small();
        paste = new UiButton("Paste", 0, 0, 78, 21, this::paste).identity("color/"+name+"/paste").small().hint(PASTE_HELP);
        paste.setTooltip(Tooltip.create(Ui.label(PASTE_HELP)));
        sync = new UiButton("Sync", 0, 0, 48, 21, UiButton.Kind.NORMAL, () -> {
            finishHex();
            session.change(toggleLink);
            UiSounds.toggled(linked.getAsBoolean());
            refreshSync(); Ui.refreshTheme();
        }).small().selected(() -> linked != null && linked.getAsBoolean())
            .hint("Link this color to Theme. Editing RGB makes it custom; alpha stays independent.");
        sync.visible = sync.active = false;
        hex = new UiEditBox(0, 0, 126, "Hex color").compact();
        hex.setMaxLength(9); syncHex(); hex.setResponder(this::editHex);
        originalPreview = new Preview("Original color", () -> initial);
        currentPreview = new Preview("Current color", this::value);
        controls = new java.util.ArrayList<>(List.of(shade, hueBar, alphaBar, hex, copy, paste, originalPreview, currentPreview));
        place();
    }
    void bind(IntSupplier synced, BiConsumer<Integer, Boolean> changed) { this.synced = synced; this.changed = changed; refreshSync(); }
    void syncRole(me.xchro.neon.config.ThemeColor role) {
        sync(role == null ? null : () -> Neon.config.theme.syncedColors.contains(role),
            () -> Neon.config.theme.sync(role, !Neon.config.theme.syncedColors.contains(role)));
        sync.hint("Link this color to Theme. Editing RGB makes it custom; alpha stays independent.");
    }
    ColorPicker independent() { themeAlpha = false; return this; }
    void sync(BooleanSupplier linked, Runnable toggle) {
        this.linked = linked; toggleLink = toggle; sync.visible = sync.active = linked != null;
        sync.hint("Follow the theme for this color. Editing it makes it custom.");
        controls.remove(sync); if (linked != null) controls.add(sync); place();
    }
    void layout(int x, int y, int width, int height, int visibleWidth) {
        this.visibleWidth = Math.max(0, visibleWidth);
        setWidth(width); setHeight(height);
        square = Math.max(24, Math.min(width, height - 86));
        setX(x); setY(y); place();
    }
    @Override public boolean isMouseOver(double x, double y) {
        return active && x < getX() + visibleWidth && super.isMouseOver(x, y);
    }
    void finish() { finishHex(); setDragging(false); session.end(); }
    boolean draggingPointer() { return isDragging(); }
    int value() { return Math.round(alpha * 255) << 24 | Color.HSBtoRGB(hue, saturation, brightness) & 0xffffff; }
    private String formatted() { return String.format(Locale.ROOT,"#%06X%02X", value() & 0xffffff, value() >>> 24); }
    private void copy() {
        finishHex();var keyboard=Minecraft.getInstance().keyboardHandler;String value=formatted();
        boolean changed=!value.equals(keyboard.getClipboard());
        if(changed)keyboard.setClipboard(value);
        if(value.equals(keyboard.getClipboard())){copy.acknowledge("Copied");if(changed)UiSounds.cue(Cue.CONFIRM);}
    }
    private void set(int color) {
        float[] hsv = Color.RGBtoHSB(color >> 16 & 255, color >> 8 & 255, color & 255, null);
        if (hsv[1] > 0) hue = hsv[0]; if (hsv[2] > 0) saturation = hsv[1]; brightness = hsv[2]; alpha = (color >>> 24) / 255f;
    }
    private void refreshSync() {
        // Do not overwrite a focused hex draft or a gesture with an animated theme frame.
        if (hex != null && hex.isFocused() || isDragging()) return;
        int color = synced.getAsInt(); lastApplied = color;
        if (color != value()) { set(color); syncHex(); }
    }
    private void syncHex() {
        if (hex == null) return;
        syncingHex = true; hex.setValue(formatted()); hex.invalid(false); syncingHex = false;
    }
    private void editHex(String text) {
        if (syncingHex) return;
        if (hexStart == null) hexStart = value();
        var parsed = text.matches("#?[0-9a-fA-F]{6}([0-9a-fA-F]{2})?") ? me.xchro.neon.core.ColorValue.parse(text.startsWith("#") ? text : "#" + text) : OptionalInt.empty();
        hex.invalid(parsed.isEmpty());
        validHex = parsed.isPresent();
        if (parsed.isPresent()) { session.begin(); set(parsed.getAsInt()); apply(); }
    }
    private void finishHex() { finishHex(false); }
    private void finishHex(boolean confirm) {
        boolean changed = hexStart != null && validHex && hexStart != value();
        hexStart = null; validHex = false;
        session.end(); if (hex != null) { hex.setFocused(false); syncHex(); }
        if (confirm && changed) UiSounds.cue(Cue.CONFIRM);
    }
    private void apply() {
        int color = value();
        if (themeAlpha) Neon.config.theme.alphaEnabled = true;
        changed.accept(color, ((lastApplied ^ color) & 0xffffff) != 0); lastApplied = color;
        Ui.refreshTheme(); pasteFeedback(false);
        if (hex != null && !hex.isFocused()) syncHex();
    }
    private void paste() {
        finishHex();
        var color = me.xchro.neon.core.ColorValue.parse(Minecraft.getInstance().keyboardHandler.getClipboard());
        pasteFeedback(color.isEmpty());
        if (color.isPresent() && color.getAsInt() != value()) {
            session.change(() -> { set(color.getAsInt()); apply(); }); UiSounds.cue(Cue.CONFIRM);
        }
    }
    private void pasteFeedback(boolean invalid) {
        if (invalidPaste == invalid) return;
        invalidPaste = invalid;
        String explanation = invalid ? "No valid color found. " + PASTE_HELP : PASTE_HELP;
        paste.status(invalid ? "Invalid" : null, Ui.RED).hint(explanation);
        paste.setTooltip(Tooltip.create(Ui.label(explanation)));
    }
    private void place() {
        if (copy == null) return;
        int x = getX(), y = getY(), w = getWidth();
        shade.setX(x + (w-square)/2); shade.setY(y); shade.setWidth(square); shade.setHeight(square);
        hueBar.setX(x); hueBar.setY(shade.getBottom() + 5); hueBar.setWidth(w); hueBar.setHeight(8);
        alphaBar.setX(x); alphaBar.setY(hueBar.getY() + 12); alphaBar.setWidth(w); alphaBar.setHeight(8);
        if (hex != null) { hex.setX(x + 38); hex.setY(alphaBar.getY() + 21); hex.setWidth(w - 42); }
        if (originalPreview != null) {
            originalPreview.setX(x); originalPreview.setY(hex.getY()-3);
            currentPreview.setX(x+16); currentPreview.setY(hex.getY()-3);
        }
        int buttonY = hex.getY() + 25, buttonWidth = linked == null ? (w - 6) / 2 : (w - 12) / 3;
        copy.setX(x); copy.setY(buttonY); copy.setWidth(buttonWidth);
        paste.setX(copy.getRight() + 6); paste.setY(buttonY); paste.setWidth(buttonWidth);
        sync.setX(paste.getRight() + 6); sync.setY(buttonY); sync.setWidth(buttonWidth);
    }
    @Override public void setX(int x) { super.setX(x); place(); }
    @Override public void setY(int y) { super.setY(y); place(); }
    @Override public List<AbstractWidget> children() { return controls; }
    @Override protected int contentHeight() { return getHeight(); }
    @Override protected double scrollRate() { return 0; }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean twice) {
        if (!isMouseOver(e.x(),e.y())) return false;
        if (!hex.isMouseOver(e.x(),e.y())) finishHex(true);
        super.mouseClicked(e,twice); return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) {
        boolean handled = super.mouseReleased(e); session.end(); return handled;
    }
    @Override public boolean keyPressed(KeyEvent e) {
        if (hex.isFocused() && (e.key()==GLFW.GLFW_KEY_ENTER || e.key()==GLFW.GLFW_KEY_KP_ENTER || e.key()==GLFW.GLFW_KEY_ESCAPE)) { finishHex(e.key()!=GLFW.GLFW_KEY_ESCAPE); return true; }
        return super.keyPressed(e);
    }
    boolean editingHex() { return hex.isFocused(); }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        refreshSync(); place();
        for(var control:controls) control.extractRenderState(g,mx,my,delta);
        if(invalidPaste) Ui.stroke(g, paste.getX(),paste.getY(),paste.getWidth(),paste.getHeight(),Ui.RED);
    }
    static void swatch(GuiGraphicsExtractor g,int x,int y,int w,int h,int color,int border) {
        checker(g,x,y,w,h); Ui.rect(g,x,y,w,h,color); Ui.stroke(g, x,y,w,h,border);
    }
    private static void checker(GuiGraphicsExtractor g,int x,int y,int w,int h) {
        UiEffects.checker(g,x,y,w,h);
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) { if(getFocused() instanceof AbstractWidget w) w.updateNarration(output); }
    private static final class Preview extends AbstractWidget {
        private final IntSupplier color;
        Preview(String name, IntSupplier color) {
            super(0,0,12,21,Ui.label(name)); this.color=color; active=false;
            setTooltip(Tooltip.create(Ui.label(name)));
        }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
            swatch(g,getX(),getY(),getWidth(),getHeight(),color.getAsInt(),Ui.LINE);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { output.add(NarratedElementType.TITLE,getMessage()); }
    }
    private final class Picker extends AbstractWidget {
        private final int kind;
        Picker(String name,int kind) { super(0,0,1,1,Ui.label(name)); this.kind=kind; }
        private void move(double x,double y) {
            float fraction=(float)Math.clamp((x-getX())/(getWidth()-1),0,1);
            if(kind==1){ saturation=fraction; brightness=1-(float)Math.clamp((y-getY())/(getHeight()-1),0,1); }
            else if(kind==2)hue=fraction; else ColorPicker.this.alpha=fraction;
            apply();
        }
        @Override public void onClick(MouseButtonEvent e,boolean twice){ session.end(); session.begin(); move(e.x(),e.y()); }
        @Override protected void onDrag(MouseButtonEvent e,double dx,double dy){move(e.x(),e.y());}
        @Override public void onRelease(MouseButtonEvent e){session.end();}
        @Override public boolean keyPressed(KeyEvent e){
            int key=e.key(); if(key!=GLFW.GLFW_KEY_LEFT&&key!=GLFW.GLFW_KEY_RIGHT&&key!=GLFW.GLFW_KEY_UP&&key!=GLFW.GLFW_KEY_DOWN)return false;
            float step=key==GLFW.GLFW_KEY_RIGHT||key==GLFW.GLFW_KEY_UP?.01f:-.01f;
            session.change(()->{if(kind==2)hue=Math.clamp(hue+step,0,1);else if(kind==3)ColorPicker.this.alpha=Math.clamp(ColorPicker.this.alpha+step,0,1);else if(key==GLFW.GLFW_KEY_LEFT||key==GLFW.GLFW_KEY_RIGHT)saturation=Math.clamp(saturation+step,0,1);else brightness=Math.clamp(brightness+step,0,1);apply();});return true;
        }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();
            if(kind==1){ int color=Color.HSBtoRGB(hue,1,1); UiEffects.gradient(g,x,y,w,h,0xffffffff,color,color,0xffffffff);UiEffects.gradient(g,x,y,w,h,0,0,0xff000000,0xff000000);
                int px=x+Math.round(saturation*(w-1)),py=y+Math.round((1-brightness)*(h-1));Ui.stroke(g, px-3,py-3,7,7,0xff000000);Ui.stroke(g, px-2,py-2,5,5,0xffffffff);
            }else{
                if(kind==2)for(int i=0;i<6;i++){int a=Color.HSBtoRGB(i/6f,1,1),b=Color.HSBtoRGB((i+1)/6f,1,1);UiEffects.gradient(g,x+w*i/6f,y,w/6f,h,a,b,b,a);}
                else{checker(g,x,y,w,h);int rgb=value()&0xffffff;UiEffects.gradient(g,x,y,w,h,rgb,rgb|0xff000000,rgb|0xff000000,rgb);}
                int px=x+Math.round((kind==2?hue:ColorPicker.this.alpha)*(w-1));Ui.stroke(g, px-2,y-2,5,h+4,0xff000000);Ui.stroke(g, px-1,y-1,3,h+2,0xffffffff);
            }
            if(isFocused()&&Minecraft.getInstance().getLastInputType().isKeyboard())Ui.stroke(g, x-3,y-3,w+6,h+6,Ui.BLUE);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output){output.add(NarratedElementType.TITLE,getMessage());output.add(NarratedElementType.USAGE,"Arrow keys adjust the color.");}
    }
}

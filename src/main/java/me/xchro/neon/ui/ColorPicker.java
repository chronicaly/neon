package me.xchro.neon.ui;

import java.awt.Color;
import java.util.*;
import java.util.function.*;
import me.xchro.neon.Neon;
import me.xchro.neon.core.TimedValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.*;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Unity ColorPicker/ClientScreen port: one inline fold, live HSV/alpha, original/current, Copy/Paste. */
final class ColorPicker extends AbstractContainerWidget {
    private final SettingsSession session;
    private final int initial;
    private IntSupplier synced;
    private IntConsumer changed;
    private float hue, saturation, brightness, alpha;
    private final TimedValue opening = new TimedValue(0);
    private boolean open = true;
    private final Picker shade = new Picker("Saturation and brightness", 1), hueBar = new Picker("Hue", 2), alphaBar = new Picker("Opacity", 3);
    private final UiButton copy, paste;
    private final List<AbstractWidget> controls;
    private boolean invalidPaste;
    ColorPicker(int width, int availableHeight, int color, SettingsSession session, IntSupplier synced, IntConsumer changed) {
        super(0, 0, 128, 0, Ui.label("Color picker"));
        this.initial = color; this.session = session; this.synced = synced; this.changed = changed; set(color);
        copy = new UiButton("Copy", 0, 0, 40, 18, UiButton.Kind.KEY, () -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(String.format(Locale.ROOT,"#%06X%02X", value() & 0xffffff, value() >>> 24));
        }).small();
        paste = new UiButton("Paste", 0, 0, 40, 18, UiButton.Kind.KEY, this::paste).small();
        controls = List.of(shade, hueBar, alphaBar, copy, paste);
        resize(width, availableHeight); opening.to(1, System.nanoTime(), 150_000_000L);
    }
    void bind(IntSupplier synced, IntConsumer changed) { this.synced = synced; this.changed = changed; refreshSync(); }
    void open(boolean value) { session.end(); open = value; opening.to(value ? 1 : 0, System.nanoTime(), 150_000_000L); }
    boolean closed() { return !open && getHeight() == 0; }
    boolean tick() {
        int next = (int) Math.round((getWidth() + 60) * opening.to(open ? 1 : 0, System.nanoTime(), 150_000_000L));
        boolean changed = next != getHeight(); setHeight(next); return changed;
    }
    void resize(int width, int availableHeight) {
        // Keep Unity's square picker and actual control sizes. At minimum window sizes the column scrolls.
        int minimum = Math.max(128, 66 + 2 * (Math.max(UiFont.width("Copy",10),UiFont.width("Paste",10)) + 4));
        setWidth(Math.min(width, Math.clamp(availableHeight - 60, minimum, 180))); tick(); place();
    }
    int top() { return getY(); }
    int value() { return Math.round(alpha * 255) << 24 | Color.HSBtoRGB(hue, saturation, brightness) & 0xffffff; }
    private void set(int color) {
        float[] hsv = Color.RGBtoHSB(color >> 16 & 255, color >> 8 & 255, color & 255, null);
        if (hsv[1] > 0) hue = hsv[0]; if (hsv[2] > 0) saturation = hsv[1]; brightness = hsv[2]; alpha = (color >>> 24) / 255f;
    }
    private void refreshSync() { int color = synced.getAsInt(); if (color != value()) set(color); }
    private void apply() { Neon.config.theme.alphaEnabled = true; changed.accept(value()); Ui.refreshTheme(); invalidPaste = false; }
    private void paste() {
        var color = me.xchro.neon.core.ColorValue.parse(Minecraft.getInstance().keyboardHandler.getClipboard());
        invalidPaste = color.isEmpty();
        if (color.isPresent()) session.change(() -> { set(color.getAsInt()); apply(); });
    }
    private void place() {
        if (copy == null) return;
        int x = getX(), y = getY(), w = getWidth(), size = w - 16;
        shade.setX(x + 8); shade.setY(y + 8); shade.setWidth(size); shade.setHeight(size);
        hueBar.setX(x + 8); hueBar.setY(shade.getBottom() + 8); hueBar.setWidth(size); hueBar.setHeight(10);
        alphaBar.setX(x + 8); alphaBar.setY(hueBar.getBottom() + 8); alphaBar.setWidth(size); alphaBar.setHeight(10);
        int buttonY = alphaBar.getBottom() + 8, buttonWidth = (w - 66) / 2;
        copy.setX(x + 52); copy.setY(buttonY); copy.setWidth(buttonWidth);
        paste.setX(copy.getRight() + 6); paste.setY(buttonY); paste.setWidth(x + w - 8 - paste.getX());
    }
    @Override public void setX(int x) { super.setX(x); place(); }
    @Override public void setY(int y) { super.setY(y); place(); }
    @Override public List<AbstractWidget> children() { return controls; }
    @Override protected int contentHeight() { return getHeight(); }
    @Override protected double scrollRate() { return 0; }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean twice) { return open && isMouseOver(e.x(),e.y()) && super.mouseClicked(e,twice); }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        refreshSync(); place();
        g.enableScissor(getX(),getY(),getRight(),getBottom());
        Ui.rect(g,getX(),getY(),getWidth(),getHeight(),Ui.PANEL); Ui.stroke(g, getX(),getY(),getWidth(),getHeight(),Ui.LINE);
        for(var control:controls) control.extractRenderState(g,mx,my,delta);
        swatch(g,getX()+8,copy.getY(),18,18,initial,Ui.MUTED);
        swatch(g,getX()+28,copy.getY(),18,18,value(),Ui.BLUE);
        if(invalidPaste) Ui.stroke(g, paste.getX(),paste.getY(),paste.getWidth(),paste.getHeight(),Ui.RED);
        g.disableScissor();
    }
    static void swatch(GuiGraphicsExtractor g,int x,int y,int w,int h,int color,int border) {
        checker(g,x,y,w,h); Ui.rect(g,x,y,w,h,color); Ui.stroke(g, x,y,w,h,border);
    }
    private static void checker(GuiGraphicsExtractor g,int x,int y,int w,int h) {
        for(int py=0;py<h;py+=4) for(int px=0;px<w;px+=4)
            Ui.rect(g,x+px,y+py,Math.min(4,w-px),Math.min(4,h-py),((px+py)/4&1)==0?0xffabb6ba:0xff617077);
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) { if(getFocused() instanceof AbstractWidget w) w.updateNarration(output); }
    private final class Picker extends AbstractWidget {
        private final int kind;
        Picker(String name,int kind) { super(0,0,1,1,Ui.label(name)); this.kind=kind; }
        private void move(double x,double y) {
            float fraction=(float)Math.clamp((x-getX())/(getWidth()-1),0,1);
            if(kind==1){ saturation=fraction; brightness=1-(float)Math.clamp((y-getY())/(getHeight()-1),0,1); }
            else if(kind==2)hue=fraction; else alpha=fraction;
            apply();
        }
        @Override public void onClick(MouseButtonEvent e,boolean twice){ session.end(); session.begin(); move(e.x(),e.y()); }
        @Override protected void onDrag(MouseButtonEvent e,double dx,double dy){move(e.x(),e.y());}
        @Override public void onRelease(MouseButtonEvent e){session.end();}
        @Override public boolean keyPressed(KeyEvent e){
            int key=e.key(); if(key!=GLFW.GLFW_KEY_LEFT&&key!=GLFW.GLFW_KEY_RIGHT&&key!=GLFW.GLFW_KEY_UP&&key!=GLFW.GLFW_KEY_DOWN)return false;
            float step=key==GLFW.GLFW_KEY_RIGHT||key==GLFW.GLFW_KEY_UP?.01f:-.01f;
            session.change(()->{if(kind==2)hue=Math.clamp(hue+step,0,1);else if(kind==3)alpha=Math.clamp(alpha+step,0,1);else if(key==GLFW.GLFW_KEY_LEFT||key==GLFW.GLFW_KEY_RIGHT)saturation=Math.clamp(saturation+step,0,1);else brightness=Math.clamp(brightness+step,0,1);apply();});return true;
        }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();
            if(kind==1){ int color=Color.HSBtoRGB(hue,1,1); UiEffects.gradient(g,x,y,w,h,0xffffffff,color,color,0xffffffff);UiEffects.gradient(g,x,y,w,h,0,0,0xff000000,0xff000000);
                int px=x+Math.round(saturation*(w-1)),py=y+Math.round((1-brightness)*(h-1));Ui.stroke(g, px-3,py-3,7,7,0xff000000);Ui.stroke(g, px-2,py-2,5,5,0xffffffff);
            }else{
                if(kind==2)for(int i=0;i<6;i++){int a=Color.HSBtoRGB(i/6f,1,1),b=Color.HSBtoRGB((i+1)/6f,1,1);UiEffects.gradient(g,x+w*i/6f,y,w/6f,h,a,b,b,a);}
                else{checker(g,x,y,w,h);int rgb=value()&0xffffff;UiEffects.gradient(g,x,y,w,h,rgb,rgb|0xff000000,rgb|0xff000000,rgb);}
                int px=x+Math.round((kind==2?hue:alpha)*(w-1));Ui.stroke(g, px-2,y-2,5,h+4,0xff000000);Ui.stroke(g, px-1,y-1,3,h+2,0xffffffff);
            }
            if(isFocused()&&Minecraft.getInstance().getLastInputType().isKeyboard())Ui.stroke(g, x-3,y-3,w+6,h+6,Ui.BLUE);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output){output.add(NarratedElementType.TITLE,getMessage());output.add(NarratedElementType.USAGE,"Arrow keys adjust the color.");}
    }
}

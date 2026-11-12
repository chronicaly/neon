package me.xchro.neon.ui;

import java.awt.Color;
import java.util.*;
import java.util.function.*;
import me.xchro.neon.Neon;
import me.xchro.neon.core.TimedValue;
import me.xchro.neon.core.UiSoundPolicy.Cue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.*;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Floating color panel; live Unity HSV/alpha mechanics without moving the settings rows. */
final class ColorPicker extends AbstractContainerWidget {
    private final SettingsSession session;
    private final int initial;
    private final String name;
    private IntSupplier synced;
    private BiConsumer<Integer, Boolean> changed;
    private int lastApplied;
    private me.xchro.neon.config.ThemeColor syncRole;
    private float hue, saturation, brightness, alpha;
    private final TimedValue opening = new TimedValue(0);
    private final Picker shade = new Picker("Saturation and brightness", 1), hueBar = new Picker("Hue", 2), alphaBar = new Picker("Opacity", 3);
    private final UiButton copy, paste, sync;
    private final UiEditBox hex;
    private final List<AbstractWidget> controls;
    private boolean invalidPaste, syncingHex, draggingHeader;
    private Integer hexStart;
    private boolean validHex;
    private int square = 120, anchorX, anchorY, offsetX, offsetY, viewportWidth, viewportHeight;
    private double grabX, grabY;
    ColorPicker(String name, int color, SettingsSession session, IntSupplier synced, BiConsumer<Integer, Boolean> changed) {
        super(0, 0, 184, 276, Ui.label("Color picker"));
        this.name = name; this.initial = color; this.lastApplied = color; this.session = session; this.synced = synced; this.changed = changed; set(color);
        copy = new UiButton("Copy", 0, 0, 78, 21, () -> {
            finishHex();
            var keyboard = Minecraft.getInstance().keyboardHandler;
            if (!formatted().equals(keyboard.getClipboard())) { keyboard.setClipboard(formatted()); UiSounds.cue(Cue.CONFIRM); }
        }).small();
        paste = new UiButton("Paste", 0, 0, 78, 21, this::paste).small();
        sync = new UiButton("Sync", 0, 0, 48, 21, UiButton.Kind.SEGMENT, () -> {
            finishHex();
            session.change(() -> Neon.config.theme.sync(syncRole, !Neon.config.theme.syncedColors.contains(syncRole)));
            UiSounds.toggled(Neon.config.theme.syncedColors.contains(syncRole));
            refreshSync(); Ui.refreshTheme();
        }).small().selected(() -> syncRole != null && Neon.config.theme.syncedColors.contains(syncRole))
            .hint("Link this color to Theme. Editing RGB makes it custom; alpha stays independent.");
        sync.visible = sync.active = false;
        hex = new UiEditBox(0, 0, 126, "Hex color").compact();
        hex.setMaxLength(9); syncHex(); hex.setResponder(this::editHex);
        controls = new java.util.ArrayList<>(List.of(shade, hueBar, alphaBar, hex, copy, paste));
        place(); opening.to(1, System.nanoTime(), 120_000_000L);
    }
    void bind(IntSupplier synced, BiConsumer<Integer, Boolean> changed) { this.synced = synced; this.changed = changed; refreshSync(); }
    void syncRole(me.xchro.neon.config.ThemeColor role) {
        syncRole = role; sync.visible = sync.active = role != null;
        controls.remove(sync); if (role != null) controls.add(sync); place();
    }
    void anchor(ScreenRectangle menu, int width, int height) {
        viewportWidth = width; viewportHeight = height;
        // Keep the independent display density; only shorten the square on unusually short displays.
        square = Math.clamp(height - 164, 24, 120);
        setHeight(square + 156);
        anchorX = menu.right() + 6;
        if (anchorX + getWidth() > width - 4 && menu.left() - getWidth() - 6 >= 4)
            anchorX = menu.left() - getWidth() - 6;
        anchorX = Math.clamp(anchorX, 4, Math.max(4, width - getWidth() - 4));
        anchorY = Math.clamp(menu.top(), 4, Math.max(4, height - getHeight() - 4));
        position(anchorX + offsetX, anchorY + offsetY);
    }
    private void position(int x, int y) {
        setX(Math.clamp(x, 4, Math.max(4, viewportWidth - getWidth() - 4)));
        setY(Math.clamp(y, 4, Math.max(4, viewportHeight - getHeight() - 4)));
    }
    void finish() { finishHex(); draggingHeader = false; setDragging(false); session.end(); }
    boolean draggingPointer() { return draggingHeader || isDragging(); }
    int value() { return Math.round(alpha * 255) << 24 | Color.HSBtoRGB(hue, saturation, brightness) & 0xffffff; }
    private String formatted() { return String.format(Locale.ROOT,"#%06X%02X", value() & 0xffffff, value() >>> 24); }
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
        Neon.config.theme.alphaEnabled = true; changed.accept(color, ((lastApplied ^ color) & 0xffffff) != 0); lastApplied = color;
        Ui.refreshTheme(); invalidPaste = false;
        if (hex != null && !hex.isFocused()) syncHex();
    }
    private void paste() {
        finishHex();
        var color = me.xchro.neon.core.ColorValue.parse(Minecraft.getInstance().keyboardHandler.getClipboard());
        invalidPaste = color.isEmpty();
        if (color.isPresent() && color.getAsInt() != value()) {
            session.change(() -> { set(color.getAsInt()); apply(); }); UiSounds.cue(Cue.CONFIRM);
        }
    }
    private void place() {
        if (copy == null) return;
        int x = getX(), y = getY(), w = getWidth();
        shade.setX(x + 10); shade.setY(y + 36); shade.setWidth(square); shade.setHeight(square);
        hueBar.setX(x + 45); hueBar.setY(shade.getBottom() + 15); hueBar.setWidth(w - 89); hueBar.setHeight(7);
        alphaBar.setX(hueBar.getX()); alphaBar.setY(hueBar.getY() + 22); alphaBar.setWidth(hueBar.getWidth()); alphaBar.setHeight(7);
        if (hex != null) { hex.setX(x + 40); hex.setY(alphaBar.getY() + 25); hex.setWidth(w - 54); }
        int buttonY = y + getHeight() - 31, buttonWidth = syncRole == null ? (w - 26) / 2 : (w - 32) / 3;
        copy.setX(x + 10); copy.setY(buttonY); copy.setWidth(buttonWidth);
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
        if (e.button() == 0 && e.y() < getY() + 26) {
            draggingHeader = true; grabX = e.x() - getX(); grabY = e.y() - getY(); return true;
        }
        super.mouseClicked(e,twice); return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (draggingHeader) { position((int)Math.round(e.x()-grabX),(int)Math.round(e.y()-grabY)); offsetX=getX()-anchorX; offsetY=getY()-anchorY; return true; }
        return super.mouseDragged(e,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) {
        boolean moved = draggingHeader; draggingHeader = false;
        boolean handled = super.mouseReleased(e); session.end(); return moved || handled;
    }
    @Override public boolean keyPressed(KeyEvent e) {
        if (hex.isFocused() && (e.key()==GLFW.GLFW_KEY_ENTER || e.key()==GLFW.GLFW_KEY_KP_ENTER || e.key()==GLFW.GLFW_KEY_ESCAPE)) { finishHex(e.key()!=GLFW.GLFW_KEY_ESCAPE); return true; }
        return super.keyPressed(e);
    }
    boolean editingHex() { return hex.isFocused(); }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        refreshSync(); place();
        float previousOpacity = Ui.opacity;
        Ui.opacity *= (float)opening.to(1, System.nanoTime(), 120_000_000L);
        g.nextStratum();
        Ui.clip(g,getX(),getY(),getRight(),getBottom());
        // Opaque floating surface prevents underlying controls showing through when it must overlap.
        Ui.rect(g,getX(),getY(),getWidth(),getHeight(),Ui.PANEL|0xff000000); Ui.stroke(g, getX(),getY(),getWidth(),getHeight(),UiEffects.mix(Ui.LINE,Ui.OUTLINE,.45));
        UiEffects.surfaceLight(g,getX()+1,getY()+1,getWidth()-2,getHeight()-2,Ui.PANEL|0xff000000);
        Ui.stroke(g,getX(),getY(),getWidth(),1,Ui.OUTLINE);
        Ui.text(g,name,getX()+10,getY()+8,Ui.TEXT);
        swatch(g,getRight()-18,getY()+9,8,8,value(),Ui.MUTED);
        Ui.rect(g,getX()+1,getY()+26,getWidth()-2,1,Ui.LINE);
        int previewX=getRight()-46, previewSize=Math.min(36,Math.max(8,(square-34)/2));
        UiFont.draw(g,"Original",previewX,shade.getY(),Ui.MUTED,8);
        swatch(g,previewX,shade.getY()+14,36,previewSize,initial,Ui.MUTED);
        int currentY=shade.getY()+square/2+3;
        UiFont.draw(g,"Current",previewX,currentY,Ui.MUTED,8);
        swatch(g,previewX,currentY+14,36,previewSize,value(),Ui.MUTED);
        Ui.small(g,"Hue",getX()+10,hueBar.getY()-2,Ui.TEXT);
        Ui.small(g,"Alpha",getX()+10,alphaBar.getY()-2,Ui.TEXT);
        String hueValue=Math.round(hue*360)+"°", alphaValue=Math.round(alpha*100)+"%";
        UiFont.draw(g,hueValue,getRight()-10-UiFont.width(hueValue,9),hueBar.getY()-2,Ui.MUTED,9);
        UiFont.draw(g,alphaValue,getRight()-10-UiFont.width(alphaValue,9),alphaBar.getY()-2,Ui.MUTED,9);
        Ui.small(g,"Hex",getX()+10,hex.getY()+1,Ui.TEXT);
        for(var control:controls) control.extractRenderState(g,mx,my,delta);
        if(invalidPaste) Ui.stroke(g, paste.getX(),paste.getY(),paste.getWidth(),paste.getHeight(),Ui.RED);
        g.disableScissor();
        Ui.opacity = previousOpacity;
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

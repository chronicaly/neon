package me.xchro.neon.ui;

import java.util.function.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class UiButton extends Button {
    public enum Kind { NORMAL, PRIMARY, QUIET, NAV, SWITCH, KEY, CARD, SEGMENT, SWATCH, MODULE, MODULE_TOGGLE, DISCLOSURE }
    private final Kind kind;
    private BooleanSupplier selected = () -> false;
    private IntSupplier swatch = () -> Ui.BLUE;
    private String description = "";
    private String statusLabel;
    private int statusColor;
    private boolean small;
    private boolean wrapped;
    private boolean centeredNavigation;
    private java.util.List<String> textLines = java.util.List.of();
    private float hoverAmount, switchAmount = -1;
    private me.xchro.neon.core.UiMotion hoverMotion = new me.xchro.neon.core.UiMotion(0);
    private me.xchro.neon.core.UiMotion switchMotion = new me.xchro.neon.core.UiMotion(0);
    private me.xchro.neon.core.UiMotion selectionMotion;
    private String identity;
    UiButton identity(String key) { identity = key; return this; }
    String identity() { return identity != null ? identity : kind + ":" + (description.isEmpty() ? getMessage().getString() : description + ":" + (kind == Kind.SEGMENT ? getMessage().getString() : "")); }
    void inherit(UiButton old) { hoverMotion = old.hoverMotion; switchMotion = old.switchMotion; switchAmount = old.switchAmount; selectionMotion = old.selectionMotion; }
    protected double selectionFraction(boolean on) {
        if (selectionMotion == null) selectionMotion = new me.xchro.neon.core.UiMotion(on ? 1 : 0);
        return selectionMotion.to(on ? 1 : 0, System.nanoTime(), 100_000_000L);
    }
    public UiButton(String text, int x, int y, int w, int h, Runnable action) { this(text, x, y, w, h, Kind.NORMAL, action); }
    public UiButton(String text, int x, int y, int w, int h, Kind kind, Runnable action) {
        super(x, y, w, h, Ui.label(text), b -> UiSounds.input(action), DEFAULT_NARRATION); this.kind = kind;
    }
    public UiButton selected(BooleanSupplier value) { selected = value; return this; }
    public UiButton swatch(IntSupplier value) { swatch = value; return this; }
    public UiButton hint(String value) { description = value; return this; }
    UiButton status(String label, int color) { statusLabel = label; statusColor = color; return this; }
    public UiButton small() { small = true; return this; }
    public UiButton centeredNavigation() { centeredNavigation = true; return this; }
    public UiButton wrapText() { wrapped = true; wrap(); return this; }
    private void wrap() {
        if (!wrapped) return;
        var lines = new java.util.ArrayList<String>(); String remaining = getMessage().getString();
        int available = Math.max(1,getWidth()-18);
        while (!remaining.isEmpty()) {
            int end=0;
            while(end<remaining.length()) { int next=remaining.offsetByCodePoints(end,1); if(end>0 && UiFont.width(remaining.substring(0,next),11)>available)break; end=next; }
            if(end<remaining.length()) { int space=remaining.lastIndexOf(' ',end); if(space>0)end=space; }
            lines.add(remaining.substring(0,end)); remaining=remaining.substring(end).stripLeading();
        }
        textLines=java.util.List.copyOf(lines); setHeight(Math.max(29,14*lines.size()+12));
    }
    @Override public void setWidth(int width) { super.setWidth(width); wrap(); }
    public String hint() { return description; }
    protected final void feedback(net.minecraft.client.input.InputWithModifiers event) {
        if (active && visible && net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof PixelScreen screen) screen.press(identity(), event);
    }
    final double pressFraction() {
        return net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof PixelScreen screen ? screen.pressAmount(identity()) : 0;
    }
    @Override public void onPress(net.minecraft.client.input.InputWithModifiers event) { feedback(event); super.onPress(event); }
    final void acknowledge(String label) { if(net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof PixelScreen screen)screen.acknowledge(identity(),label); }
    private void actionText(GuiGraphicsExtractor g,String text,int x,int y,int color,int size) {
        if (statusLabel != null) {
            UiFont.draw(g,statusLabel,getX()+(getWidth()-UiFont.width(statusLabel,size))/2,y,statusColor,size);
            return;
        }
        var feedback=net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof PixelScreen screen?screen.acknowledgement(identity()):null;
        if(feedback==null){UiFont.draw(g,text,x,y,color,size);return;}
        double amount=feedback.amount(System.nanoTime());
        UiFont.draw(g,text,x,y,Ui.alpha(color,(int)Math.round((color>>>24)*(1-amount))),size);
        String label=feedback.label();
        while(!label.isEmpty()&&UiFont.width(label,size)>getWidth()-4)label=label.substring(0,label.offsetByCodePoints(label.length(),-1));
        UiFont.draw(g,label,getX()+(getWidth()-UiFont.width(label,size))/2,y,Ui.alpha(Ui.BLUE,(int)Math.round((Ui.BLUE>>>24)*amount)),size);
    }
    protected float hoverFraction() {
        boolean focused = isFocused() && net.minecraft.client.Minecraft.getInstance().getLastInputType().isKeyboard();
        return (float) hoverMotion.to(active && (isHovered() || focused) ? 1 : 0, System.nanoTime(), 100_000_000L);
    }
    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager sounds) {}
    @Override public void setMessage(Component value) { super.setMessage(Ui.label(value)); wrap(); }
    @Override protected net.minecraft.network.chat.MutableComponent createNarrationMessage() {
        return super.createNarrationMessage().append(description.isEmpty() ? "" : ". " + description);
    }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
        boolean keyboardFocus = isFocused() && net.minecraft.client.Minecraft.getInstance().getLastInputType().isKeyboard();
        boolean hover = (isHovered() || keyboardFocus) && active, chosen = selected.getAsBoolean();
        long now = System.nanoTime();
        hoverAmount = hoverFraction();
        double selectedAmount = selectionFraction(chosen);
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        if (keyboardFocus) Ui.stroke(g, x, y, w, h, Ui.BLUE);
        if (kind == Kind.MODULE_TOGGLE) {
            boolean on = getMessage().getString().equals("ON");
            int tx=x+7,ty=y+8;
            if(switchAmount<0)switchMotion.snap(on?1:0);
            switchAmount=(float)switchMotion.to(on?1:0,now,100_000_000L);
            int fill=Ui.alpha(Ui.BLUE,Math.round((Ui.BLUE>>>24)*switchAmount));
            if(switchAmount>.01)Ui.glow(g,tx,ty,10,10,fill,3);
            Ui.rect(g,tx,ty,10,10,fill);Ui.stroke(g,tx,ty,10,10,UiEffects.mix(Ui.MUTED,Ui.BLUE,switchAmount));
            if(hoverAmount>.01)Ui.rect(g,x,y,w,h,Ui.alpha(Ui.BLUE,(int)(hoverAmount*12)));
            return;
        }
        if (kind == Kind.MODULE) {
            Ui.text(g,getMessage().getString(),x+1,y+7,UiEffects.mix(Ui.TEXT,Ui.BLUE,hoverAmount)); return;
        }
        if (kind == Kind.DISCLOSURE) {
            int color=UiEffects.mix(Ui.MUTED,Ui.BLUE,Math.max(hoverAmount,selectedAmount));
            Ui.disclosure(g, x + w - 11, y + (h - 13) / 2, chosen, color);
            return;
        }
        if (kind == Kind.NAV) {
            int fill = UiEffects.mix(Ui.PANEL,Ui.TEXT,.09);
            double emphasis=Math.max(hoverAmount,selectedAmount);
            if(emphasis>.001)Ui.rect(g,x,y,w,h,Ui.alpha(fill,(int)Math.round((Ui.PANEL>>>24)*.28*emphasis)));
            if(selectedAmount>.001)Ui.stroke(g,x,y,w,h,Ui.alpha(0xff7e8794,(int)(48*selectedAmount)));
            UiEffects.hoverLight(g,x,y,w,h,keyboardFocus ? x+w/2 : mx,hoverAmount);
            int color=UiEffects.mix(Ui.MUTED,Ui.TEXT,Math.max(hoverAmount,selectedAmount));
            String label = getMessage().getString();
            int start = centeredNavigation ? x + (w - 19 - Ui.width(label)) / 2 : x + 5;
            UiIcons.draw(g,UiIcons.category(label),start,y+5,color);
            Ui.text(g,label,start+19,y+6,color); return;
        }
        if (getMessage().getString().equals("‹")) {
            UiFont.draw(g, "‹", x + (w - UiFont.width("‹", 13)) / 2, y + (h - 13) / 2, hover ? Ui.BLUE : Ui.MUTED, 13);
            return;
        }
        if (kind == Kind.SWITCH) {
            boolean on = getMessage().getString().equals("ON");
            if (switchAmount < 0) switchMotion.snap(on ? 1 : 0);
            switchAmount = (float) switchMotion.to(on ? 1 : 0, now, 100_000_000L);
            int tx = x + w - 24, ty = y + (h - 13) / 2;
            double amount = active ? switchAmount : 0;
            if (amount > .01) Ui.glow(g, tx, ty, 24, 13, Ui.alpha(Ui.BLUE,(int)((Ui.BLUE>>>24)*amount)), 5);
            Ui.rect(g, tx, ty, 24, 13, UiEffects.mix(Ui.FIELD,UiEffects.mix(Ui.FIELD,Ui.BLUE,.10),amount));
            Ui.stroke(g, tx, ty, 24, 13, UiEffects.mix(Ui.LINE,UiEffects.mix(Ui.BLUE, Ui.PANEL, .2),amount));
            Ui.rect(g, tx + 3 + Math.round(11 * switchAmount), ty + 3, 7, 7, !active ? Ui.MUTED : UiEffects.mix(0xff7a8999,Ui.BLUE,amount));
            return;
        }
        if (kind == Kind.SWATCH) {
            Ui.rect(g, x + w - 25, y + (h - 14) / 2, 25, 14, swatch.getAsInt());
            Ui.stroke(g, x + w - 25, y + (h - 14) / 2, 25, 14, hover ? Ui.BLUE : Ui.LINE); return;
        }
        boolean plain = kind == Kind.QUIET || kind == Kind.KEY || kind == Kind.CARD || kind == Kind.SEGMENT;
        double emphasis=Math.max(hoverAmount,selectedAmount);
        int fill=UiEffects.mix(UiEffects.mix(Ui.FIELD,Ui.BLUE,.04*hoverAmount+.08*selectedAmount),Ui.INK,.16*pressFraction());
        Ui.rect(g,x,y,w,h,Ui.buttonSurface(fill,emphasis));
        if (pressFraction() > .001) Ui.stroke(g,x,y,w,h,Ui.alpha(Ui.BLUE,(int)((Ui.BLUE>>>24)*.3*pressFraction())));
        if (!plain) Ui.stroke(g, x, y, w, h, chosen ? Ui.BLUE : hover ? Ui.MUTED : Ui.LINE);
        int color = !active ? Ui.MUTED : chosen ? Ui.BLUE : kind == Kind.QUIET || kind == Kind.KEY || kind == Kind.NAV || kind == Kind.SEGMENT ? Ui.MUTED : Ui.TEXT;
        if (wrapped) {
            int lineY=y+(h-14*textLines.size())/2+1;
            for(String line:textLines) { UiFont.draw(g,line,x+9,lineY,color,11); lineY+=14; }
            return;
        }
        int size = kind == Kind.NAV ? 11 : small || kind == Kind.SEGMENT || h <= 24 ? 10 : 11;
        String text = getMessage().getString();
        int inset = kind == Kind.KEY ? 2 : kind == Kind.SEGMENT ? 6 : kind == Kind.CARD ? 9 : h <= 24 ? 6 : 8;
        int available = w - (kind == Kind.CARD ? 13 : inset * 2);
        if (UiFont.width(text, size) > available) {
            while (!text.isEmpty() && UiFont.width(text + "…", size) > available) text = text.substring(0, text.offsetByCodePoints(text.length(), -1));
            text += "…";
        }
        int tx = kind == Kind.NAV || kind == Kind.CARD ? x + inset : x + (w - UiFont.width(text, size)) / 2;
        if (active && getMessage().getStyle().getColor() != null) color = 0xff000000 | getMessage().getStyle().getColor().getValue();
        actionText(g, text, tx, y + (h - size) / 2, color, size);
    }
}

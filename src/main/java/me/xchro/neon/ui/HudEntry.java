package me.xchro.neon.ui;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

/** Unity's single-line row rendering, adapted to Neon's native-resolution typography. */
final class HudEntry extends UiButton {
    private final Runnable select;
    private final BooleanSupplier enabled, selected;
    private int depth;
    private boolean toggle;
    HudEntry(String name, int width, BooleanSupplier enabled, BooleanSupplier selected, Runnable toggle, Runnable select) {
        super(name, 0, 0, width, 22, Kind.CARD, toggle);
        this.enabled = enabled; this.selected = selected; this.select = select;
        hint(select == null ? "Left click changes the setting." : "Left click changes the setting; right click opens its options.");
    }
    HudEntry depth(int value) { depth = value; return this; }
    HudEntry toggleRow(boolean value) { toggle=value;return this; }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (select != null && event.button() == 1 && isMouseOver(event.x(), event.y())) { feedback(event);select.run(); return true; }
        return super.mouseClicked(event, twice);
    }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
        boolean on = enabled.getAsBoolean();
        boolean open = selected.getAsBoolean();
        double enabledAmount = selectionFraction(on);
        boolean keyboardFocus = isFocused() && net.minecraft.client.Minecraft.getInstance().getLastInputType().isKeyboard();
        double amount = hoverFraction();
        int x=getX()+5,y=getY()+1,w=getWidth()-10,h=getHeight()-2;
        double emphasis=Math.max(amount,open?1:0);
        int fill=UiEffects.mix(Ui.FIELD,Ui.INK,.16*pressFraction());
        if(depth==0) {
            RoundedUi.gradient(g,x,y,w,h,Ui.buttonSurface(UiEffects.mix(fill,Ui.BLUE,.10*amount),emphasis),Ui.buttonSurface(UiEffects.mix(fill,Ui.VIOLET,.18*amount),emphasis),Ui.buttonSurface(UiEffects.mix(fill,Ui.VIOLET,.18*amount),emphasis),Ui.buttonSurface(UiEffects.mix(fill,Ui.BLUE,.10*amount),emphasis),3);
            int edge=Ui.faded(UiEffects.mix(Ui.LINE,Ui.BLUE,Math.max(.22*enabledAmount,.5*emphasis)));
            RoundedUi.stroke(g,x,y,w,h,edge,edge);
            if(toggle)Ui.rect(g,x+4,y+6,1,8,UiEffects.mix(Ui.LINE,Ui.BLUE,enabledAmount));
        } else if(emphasis>.001) RoundedUi.fill(g,x,y,w,h,Ui.buttonSurface(fill,emphasis),3);
        int tx=x+9,available=w-18-(select!=null?16:toggle?28:0);
        String label=getMessage().getString();int split=toggle?-1:label.indexOf(": ");
        if(split>=0) {
            String value=label.substring(split+2);label=label.substring(0,split);int valueWidth=Ui.width(value);
            Ui.text(g,Ui.fit(label,Math.max(1,available-valueWidth-10)),tx,getY()+6,Ui.TEXT);
            Ui.text(g,value,x+w-8-valueWidth,getY()+6,Ui.BLUE);
        } else Ui.text(g,Ui.fit(label,available),tx,getY()+6,depth==0&&toggle?UiEffects.mix(Ui.MUTED,Ui.TEXT,Math.max(enabledAmount,amount)):Ui.TEXT);
        if(select!=null)Ui.disclosure(g,x+w-9,getY()+4,open,UiEffects.mix(Ui.MUTED,Ui.BLUE,emphasis));
        else if(toggle)Ui.switchControl(g,x+w-26,getY()+5,enabledAmount,active);
        if(keyboardFocus)RoundedUi.stroke(g,x,y,w,h,Ui.faded(Ui.BLUE),Ui.faded(Ui.BLUE));
    }
}

package me.xchro.neon.ui;

import java.util.Arrays;
import java.util.function.BooleanSupplier;
import me.xchro.neon.Neon;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

final class UiBindButton extends UiButton {
    private final ClientModule module;
    private final BooleanSupplier capturing;
    private final Runnable cycle;
    private boolean showingMode;
    UiBindButton(ClientModule module, BooleanSupplier capturing, Runnable capture, Runnable cycle) {
        super(SettingsScreen.bindingName(module.key()),0,0,40,26,Kind.KEY,capture);
        this.module=module;this.capturing=capturing;this.cycle=cycle;
        identity(module.name()+"/binding");refresh();
    }
    static int measure(String label) { return UiFont.width(label,10)+6; }
    void refresh() {
        String label=capturing.getAsBoolean()?"[…]":SettingsScreen.bindingName(module.key());
        if(capturing.getAsBoolean())showingMode=false;
        if(!getMessage().getString().equals(label))setMessage(Ui.label(label));
        setWidth(measure(label));
        hint(module.mode(Neon.config).label+". Left click assigns a key; right click cycles Toggle, Hold, Reverse hold.");
    }
    void prepareHover(int mx,int my) { showingMode=!capturing.getAsBoolean()&&isMouseOver(mx,my); }
    private String displayLabel() { return showingMode?"["+module.mode(Neon.config).label+"]":getMessage().getString(); }
    int textLeft() { return getRight()-3-UiFont.width(displayLabel(),10); }
    @Override public boolean isMouseOver(double x,double y) {
        // The wider mode readout stays interactive without changing the grid or idle key target.
        return super.isMouseOver(x,y)||showingMode&&visible&&active&&x>=textLeft()-3&&x<getRight()&&y>=getY()&&y<getBottom();
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice) {
        if(event.button()==1&&isMouseOver(event.x(),event.y())){UiSounds.input(cycle);return true;}
        return super.mouseClicked(event,twice);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if(event.key()==GLFW.GLFW_KEY_MENU||event.key()==GLFW.GLFW_KEY_F10&&(event.modifiers()&GLFW.GLFW_MOD_SHIFT)!=0){UiSounds.input(cycle);return true;}
        return super.keyPressed(event);
    }
    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
        refresh();var key=module.key();
        boolean conflict=!key.isUnbound()&&Arrays.stream(Minecraft.getInstance().options.keyMappings).anyMatch(other->other!=key&&key.same(other));
        int color=capturing.getAsBoolean()||showingMode?Ui.BLUE:conflict?Ui.RED:Ui.MUTED;
        UiFont.draw(g,displayLabel(),textLeft(),getY()+8,color,10);
        if(isFocused()&&Minecraft.getInstance().getLastInputType().isKeyboard())Ui.stroke(g,getX(),getY(),getWidth(),getHeight(),Ui.BLUE);
    }
}

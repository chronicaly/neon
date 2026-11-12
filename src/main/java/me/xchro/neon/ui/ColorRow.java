package me.xchro.neon.ui;
import java.util.function.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
final class ColorRow extends UiButton {
    private final IntSupplier color;
    private final Runnable toggle;
    private final BooleanSupplier open;
    private boolean hud;
    ColorRow(String name,int width,IntSupplier color,Runnable toggle,BooleanSupplier open){super(name,0,0,width,27,Kind.SWATCH,toggle);this.color=color;this.toggle=toggle;this.open=open;}
    ColorRow hudRow(){hud=true;setHeight(22);return this;}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){
        if(event.button()==1&&isMouseOver(event.x(),event.y())){playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());toggle.run();return true;}
        return super.mouseClicked(event,twice);
    }
    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){
        float hover=hoverFraction();
        if(hud)Ui.rect(g,getX(),getY(),getWidth(),getHeight(),HudEditor.surface(Ui.PANEL,100));
        UiEffects.hoverLight(g,getX(),getY(),getWidth(),getHeight(),isFocused() ? getX()+getWidth()/2 : mx,hover);
        if(hover>.01f||open.getAsBoolean())Ui.rect(g,getX(),getY(),getWidth(),getHeight(),Ui.alpha(Ui.BLUE,(int)(9*Math.max(hover,open.getAsBoolean()?1:0))));
        int inset=hud?10:0, offset=hud?-2:0;
        Ui.text(g,Ui.fit(getMessage().getString(),getWidth()-52-inset),getX()+inset,getY()+8+offset,open.getAsBoolean()?Ui.BLUE:Ui.TEXT);
        Ui.disclosure(g,getRight()-34,getY()+7+offset,open.getAsBoolean(),Ui.MUTED);
        ColorPicker.swatch(g,getRight()-17,getY()+6+offset,14,14,color.getAsInt(),Ui.MUTED);
        if(open.getAsBoolean())Ui.stroke(g,getRight()-20,getY()+3+offset,20,20,Ui.BLUE);
    }
}

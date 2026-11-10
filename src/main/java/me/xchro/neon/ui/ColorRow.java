package me.xchro.neon.ui;
import java.util.function.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
final class ColorRow extends UiButton {
    private final IntSupplier color;
    private final Runnable toggle;
    ColorRow(String name,int width,IntSupplier color,Runnable toggle){super(name,0,0,width,27,Kind.SWATCH,toggle);this.color=color;this.toggle=toggle;}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){
        if(event.button()==1&&isMouseOver(event.x(),event.y())){playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());toggle.run();return true;}
        return super.mouseClicked(event,twice);
    }
    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){
        float hover=hoverFraction();
        if(hover>.01f)Ui.rect(g,getX(),getY(),getWidth(),getHeight(),Ui.alpha(Ui.BLUE,(int)(9*hover)));
        Ui.text(g,getMessage().getString(),getX(),getY()+8,Ui.TEXT);
        ColorPicker.swatch(g,getRight()-14,getY()+6,14,14,color.getAsInt(),Ui.MUTED);
    }
}

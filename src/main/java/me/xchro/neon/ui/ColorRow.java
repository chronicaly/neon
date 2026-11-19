package me.xchro.neon.ui;
import java.util.function.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
final class ColorRow extends UiButton {
    private final IntSupplier color;
    private final Runnable toggle;
    private final BooleanSupplier open;
    private boolean hud;
    ColorRow(String name,int width,IntSupplier color,Runnable toggle,BooleanSupplier open){super(name,0,0,width,27,Kind.SWATCH,toggle);this.color=color;this.toggle=toggle;this.open=open;}
    ColorRow hudRow(){hud=true;setHeight(22);return this;}
    static boolean underPointer(UiScroll pane, MouseButtonEvent event) {
        if (pane == null || event.button() > 1 || !pane.isMouseOver(event.x(), event.y())) return false;
        GuiEventListener target = pane;
        while (target instanceof ContainerEventHandler group)
            target = group.getChildAt(event.x(), event.y()).orElse(null);
        return target instanceof ColorRow row && row.active && row.visible;
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){
        if(event.button()==1&&isMouseOver(event.x(),event.y())){feedback(event);playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());toggle.run();return true;}
        return super.mouseClicked(event,twice);
    }
    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){
        float hover=hoverFraction();
        double emphasis=Math.max(hover,open.getAsBoolean()?1:0);
        int x=getX()+(hud?5:0),y=getY()+(hud?1:2),w=getWidth()-(hud?10:0),h=getHeight()-(hud?2:4);
        int fill=UiEffects.mix(UiEffects.mix(Ui.FIELD,Ui.BLUE,.08*emphasis),Ui.INK,.16*pressFraction());
        RoundedUi.fill(g,x,y,w,h,Ui.buttonSurface(fill,emphasis),3);
        int edge=Ui.faded(UiEffects.mix(Ui.LINE,Ui.BLUE,.55*emphasis));RoundedUi.stroke(g,x,y,w,h,edge,edge);
        int inset=hud?14:8, offset=hud?-2:0,swatchInset=hud?8:3;
        Ui.text(g,Ui.fit(getMessage().getString(),getWidth()-52-inset),getX()+inset,getY()+8+offset,open.getAsBoolean()?Ui.BLUE:Ui.TEXT);
        Ui.disclosure(g,x+w-swatchInset-27,getY()+7+offset,open.getAsBoolean(),UiEffects.mix(Ui.MUTED,Ui.BLUE,emphasis));
        ColorPicker.swatch(g,x+w-swatchInset-14,getY()+6+offset,14,14,color.getAsInt(),open.getAsBoolean()?Ui.BLUE:Ui.MUTED);
    }
}

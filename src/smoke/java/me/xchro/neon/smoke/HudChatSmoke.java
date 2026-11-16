package me.xchro.neon.smoke;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.hud.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

/** Real native chat transitions and the resulting packaged HUD surface positions. */
final class HudChatSmoke {
    private final Minecraft mc;
    private final NeonConfig original;
    private int checks,variant,stage,age,clearance;
    private String saved;
    private List<HudBounds> resting;
    private int turning;
    HudChatSmoke(Minecraft mc,int checks){this.mc=mc;this.checks=checks;original=ConfigStore.copy(Neon.config);}
    int checks(){return checks;}
    private void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;System.out.println("NEON_CHECK "+message);}
    private List<HudBounds> rendered(boolean preview)throws Exception {
        var state=new GuiRenderState();var g=new GuiGraphicsExtractor(mc,state,0,0);
        NeonHud.drawPixels(g,preview,mc.getWindow().getWidth(),mc.getWindow().getHeight());
        var bounds=new ArrayList<HudBounds>();
        state.forEachElement(element->{
            if(!element.getClass().getSimpleName().equals("Glass"))return;
            try {
                var field=element.getClass().getDeclaredField("fills");field.setAccessible(true);
                for(var surface:(List<?>)field.get(element))bounds.add(((HudGlassRenderer.Surface)surface).bounds());
            }catch(Exception error){throw new AssertionError(error);}
        },GuiRenderState.TraverseRange.ALL);
        return bounds;
    }
    private void setup(){
        mc.gui.setScreen(null);mc.getWindow().setWindowed(variant==3?640:1280,variant==3?360:800);
        mc.options.guiScale().set(variant%3+1);mc.options.framerateLimit().set(120);mc.resizeGui();
        Neon.applyConfig(new NeonConfig());var c=Neon.config;c.armor.visible=true;c.totems.visible=false;
        c.fps.visible=c.fpsBackground=true;c.fps.x=variant==3?.36:.5;c.fps.y=variant==3?1:.08;
        var coords=c.hud(HudKind.COORDINATES);coords.placement.visible=true;coords.placement.x=.02;coords.placement.y=1;
        var inventory=c.hud(HudKind.INVENTORY);inventory.placement.visible=true;inventory.placement.x=.98;inventory.placement.y=1;
        c.coordinatesDimension=false;c.coordinatesConversion=variant!=3;HudWidgets.invalidate();saved=ConfigStore.encode(c);
        var compass=c.hud(HudKind.COMPASS);compass.placement.visible=true;
        int width=mc.getWindow().getWidth(),height=mc.getWindow().getHeight();var below=HudWidgets.get(HudKind.COORDINATES).bounds(width,height,false);var above=HudWidgets.get(HudKind.COMPASS).bounds(width,height,false);
        above.move(compass.placement,below.x(),below.y()-above.height()-6,width,height);saved=ConfigStore.encode(c);
    }
    private void chat(){
        mc.gui.setScreen(new ChatScreen("",false));
        int top=mc.gui.screen().children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow().getY()-2;
        clearance=mc.getWindow().getHeight()-top*NeonHud.gameScale()+4;
    }
    private int lift(List<HudBounds> drawn,int index){return resting.get(index).y()-drawn.get(index).y();}
    private void preserved(){
        check(saved.equals(ConfigStore.encode(Neon.config)),"chat lift never changes saved HUD positions or settings");
        int width=mc.getWindow().getWidth(),height=mc.getWindow().getHeight();
        check(FpsHud.bounds(width,height).equals(resting.getFirst()),"FPS editor bounds remain at their saved position");
        check(HudWidgets.get(HudKind.COORDINATES).bounds(width,height,false).equals(resting.get(1)),"coordinate editor bounds remain at their saved position");
    }
    boolean run()throws Exception {
        if(variant==4){Neon.applyConfig(original);mc.gui.setScreen(null);return true;}
        age++;
        switch(stage){
            case 0->{setup();stage++;age=0;}
            case 1->{
                if(age<7)return false;resting=rendered(false);check(resting.size()==5,"chat fixture renders FPS, coordinates, joined compass, inventory and fixed armor");
                chat();check(rendered(false).equals(resting),"chat opening begins at the existing rendered position");stage++;age=0;
            }
            case 2->{
                var drawn=rendered(false);int shift=lift(drawn,1);
                check(shift>0&&shift<clearance,"chat opens through an intermediate HUD position");
                check(lift(drawn,2)==shift,"joined compass and coordinate panels keep their relative spacing");
                check(lift(drawn,3)==shift,"inventory and coordinate panels move together");
                check(drawn.get(4).equals(resting.get(4)),"armor panel stays fixed while chat opens");
                if(variant<3)check(drawn.getFirst().equals(resting.getFirst()),"top-edge FPS stays still while chat opens");
                stage++;age=0;
            }
            case 3->{
                if(age<7)return false;var drawn=rendered(false);
                for(int i=variant==3?0:1;i<4;i++)check(lift(drawn,i)==clearance,"bottom HUD clears native chat input at GUI scale "+NeonHud.gameScale());
                check(drawn.get(4).equals(resting.get(4)),"armor remains underneath open chat at its saved position");
                preserved();Screenshot.grab(mc.gameDirectory,"neon-chat-lift-"+variant+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});
                mc.gui.setScreen(null);check(rendered(false).equals(drawn),"chat closing starts without a position jump");stage++;age=0;
            }
            case 4->{
                var drawn=rendered(false);turning=lift(drawn,1);check(turning>0&&turning<clearance,"closing returns through an intermediate HUD position");
                long start=System.nanoTime();chat();int reopened=lift(rendered(false),1);
                int travel=(int)Math.ceil((System.nanoTime()-start)/1e9*clearance*20)+1;
                check(Math.abs(reopened-turning)<=travel,"rapid reopen preserves position within elapsed animation travel");stage++;age=0;
            }
            case 5->{
                if(age<7)return false;check(lift(rendered(false),1)==clearance,"rapid reversal settles above the chat input");
                check(rendered(true).equals(resting),"editor previews use saved placement without chat displacement");
                mc.gui.setScreen(null);stage++;age=0;
            }
            case 6->{
                if(age<7)return false;check(rendered(false).equals(resting),"closing restores exact original HUD positions");preserved();
                variant++;stage=age=0;
            }
        }
        return false;
    }
}

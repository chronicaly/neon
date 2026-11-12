package me.xchro.neon.smoke;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Native pointer checks and reference-state captures for approved option B. */
final class RowsSmoke {
    private final Minecraft mc;
    private final NeonConfig original=ConfigStore.copy(Neon.config);
    private final String lookKey=Neon.freelook.saveString(), zoomKey=Neon.zoom.saveString();
    private final Map<KeyMapping,String> otherKeys=new IdentityHashMap<>();
    private SettingsScreen menu;
    private int index,age,checks;
    RowsSmoke(Minecraft mc,int checks){this.mc=mc;this.checks=checks;for(var key:mc.options.keyMappings)otherKeys.put(key,key.saveString());setup();}
    int checks(){return checks;}
    private void check(boolean ok,String name){if(!ok)throw new AssertionError("Rows "+index+": "+name);checks++;System.out.println("NEON_CHECK "+name);}
    private List<AbstractWidget> widgets(ContainerEventHandler root){var all=new ArrayList<AbstractWidget>();for(var child:root.children()){if(child instanceof AbstractWidget w)all.add(w);if(child instanceof ContainerEventHandler c)all.addAll(widgets(c));}return all;}
    private UiButton button(String name){return widgets(menu).stream().filter(w->w instanceof UiButton&&w.getMessage().getString().equals(name)).map(w->(UiButton)w).findFirst().orElseThrow();}
    private UiButton toggle(String name){return widgets(menu).stream().filter(w->w instanceof UiButton b&&b.hint().equals("Enable "+name)).map(w->(UiButton)w).findFirst().orElseThrow();}
    private MouseButtonEvent pointer(double x,double y){return new MouseButtonEvent(menu.guiCoordinate(x),menu.guiCoordinate(y),new MouseButtonInfo(0,0));}
    private void click(AbstractWidget w){var e=pointer(w.getX()+w.getWidth()/2.0,w.getY()+w.getHeight()/2.0);check(menu.mouseClicked(e,false),"visible row target accepts native pointer: "+w.getMessage().getString());menu.mouseReleased(e);}
    private List<ExactSlider> numbers(){return widgets(menu).stream().filter(w->w instanceof ExactSlider).map(w->(ExactSlider)w).toList();}
    private int sliderCoordinate(ExactSlider slider, String method) {
        try { var coordinate = ExactSlider.class.getDeclaredMethod(method); coordinate.setAccessible(true); return (int) coordinate.invoke(slider); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private void sliderClick(ExactSlider slider, String coordinate) {
        var p = pointer(sliderCoordinate(slider, coordinate), sliderCoordinate(slider, "compactTrackY"));
        check(menu.mouseClicked(p, false), "compact slider accepts its visible track endpoint"); menu.mouseReleased(p);
    }
    private void setup(){
        new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"),Neon.SAVES);Neon.SAVES.flush().join();
        mc.getWindow().setWindowed(1280,800);mc.options.guiScale().set(index/2+1);mc.resizeGui();mc.setLastInputType(InputType.MOUSE);
        Neon.config=new NeonConfig();Neon.config.freelookEnabled=false;Neon.config.motionBlurEnabled=true;
        Neon.freelook.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_C));Neon.zoom.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Z));KeyMapping.resetMapping();
        for(var key:mc.options.keyMappings)if(key!=Neon.freelook&&key!=Neon.zoom&&(key.same(Neon.freelook)||key.same(Neon.zoom)))key.setKey(InputConstants.UNKNOWN);
        KeyMapping.resetMapping();
        mc.gui.setScreen(menu=Menus.fresh(null));
        if(index%2==1){var b=menu.menuBounds();var start=pointer(b.right()-1,b.bottom()-1);menu.mouseClicked(start,false);var end=pointer(b.right()-121,b.bottom()-71);menu.mouseDragged(end,end.x()-start.x(),end.y()-start.y());menu.mouseReleased(end);}
        click(button("Motion blur"));age=0;
    }
    boolean run(){
        age++;
        if(index>=6){
            if(index==9){
                if(age==3){mc.getWindow().setWindowed(640,480);mc.resizeGui();}
                if(age<10)return false;
                var scroll=widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiScroll")).findFirst().orElseThrow();
                var picker=widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("ColorPicker")).findFirst().orElseThrow();
                check(picker.getX()>=0&&picker.getRight()<=menu.canvasWidth()&&picker.getBottom()<=menu.canvasHeight(),"floating color picker stays on-screen when the game window shrinks");
                check(Neon.config.theme.outline==0xff73c8d3,"viewport resize preserves live color value");
                Neon.freelook.setKey(InputConstants.getKey(lookKey));Neon.zoom.setKey(InputConstants.getKey(zoomKey));
                otherKeys.forEach((key,value)->key.setKey(InputConstants.getKey(value)));KeyMapping.resetMapping();mc.options.save();
                Neon.applyConfig(original);mc.gui.setScreen(null);mc.getWindow().setWindowed(1280,800);mc.resizeGui();return true;
            }
            if(age==3&&index<8){Neon.freelook.setDown(true);Neon.zoom.setDown(true);}
            if(age<8)return false;
            if(index==6){
                check(!Neon.CAMERA.looking()&&Math.abs(Neon.CAMERA.fov(90)-90)<.01,"disabled camera modules ignore held bindings");
                Neon.freelook.setDown(false);Neon.zoom.setDown(false);Neon.config.freelookEnabled=Neon.config.zoomEnabled=true;index++;age=0;return false;
            }
            if(index==7){
                check(Neon.CAMERA.looking()&&Neon.CAMERA.fov(90)<89,"enabled camera modules still activate from their bindings");
                Neon.config.freelookEnabled=Neon.config.zoomEnabled=false;index++;age=0;return false;
            }
            check(!Neon.CAMERA.looking()&&Math.abs(Neon.CAMERA.fov(90)-90)<.01,"disabling camera modules releases active perspective and zoom");
            Neon.freelook.setDown(false);Neon.zoom.setDown(false);
            new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"),Neon.SAVES);Neon.SAVES.flush().join();
            mc.gui.setScreen(menu=Menus.fresh(null));click(button("Settings"));click(button("Colors"));click(button("Outline"));
            index=9;age=0;return false;
        }
        if(age==8){
            check(menu.menuBounds().width()==(index%2==0?440:320),"option B retains compact default/minimum width at GUI scale "+(index/2+1));
            check(button("Freelook").getY()<button("Zoom").getY()&&button("Zoom").getY()<button("Motion blur").getY(),"modules form one ordered vertical list");
            check(numbers().size()==1&&numbers().getFirst().getY()>button("Motion blur").getBottom(),"settings expand directly beneath the selected module");
            check(button("[Z]").getX()-button("Zoom").getRight()<=2,"binding sits immediately beside module name");
            int quiet=SoundAudit.neonEnables;
            click(toggle("Motion blur"));check(!Neon.config.motionBlurEnabled&&numbers().size()==1&&!numbers().getFirst().active,"square disables module without collapsing settings");
            check(SoundAudit.neonEnables==quiet,"disabling a row never requests an enable cue");
            click(toggle("Motion blur"));check(Neon.config.motionBlurEnabled&&SoundAudit.neonEnables==quiet+1,"enabling row toggle plays exactly one sound");
            click(button("[Z]"));menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_F8,0,0));check(Neon.zoom.saveString().endsWith("f8")&&numbers().getFirst().getMessage().getString().equals("10%"),"bind target captures without opening or toggling its module; motion blur defaults to 10%");
            click(button("Zoom"));check(Neon.config.zoomEnabled&&numbers().size()==2&&numbers().getFirst().getMessage().getString().equals("4x"),"module name immediately opens direct settings without toggling");
            check(widgets(menu).stream().noneMatch(w->w.getMessage().getString().equals("Reset")),"expanded modules have no Reset button");
            check(widgets(menu).stream().noneMatch(w->w.getMessage().getString().equals("Bind")),"no expanded keybind row is present");
            check(widgets(menu).stream().filter(w->w instanceof UiButton&&w.getMessage().getString().equals("[C]")).count()==1,"Freelook has exactly one header bind");
            if(index%2==0)check(numbers().stream().allMatch(s->s.getHeight()==27),"default numeric controls keep label, track and value on one compact row");
            sliderClick(numbers().getFirst(),"compactTrackLeft");check(Neon.config.zoomFactor==1,"left rendered track endpoint sets minimum");
            sliderClick(numbers().getFirst(),"compactTrackRight");check(Neon.config.zoomFactor==16,"right rendered track endpoint sets maximum");
            menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z,0,GLFW.GLFW_MOD_CONTROL));check(Neon.config.zoomFactor==1,"one Undo restores the preceding slider gesture");
            menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z,0,GLFW.GLFW_MOD_CONTROL));check(Neon.config.zoomFactor==4,"second Undo restores the original slider value");
            var exact = numbers().getFirst(); var valueClick = pointer(exact.getRight()-2,sliderCoordinate(exact,"compactTrackY"));
            check(menu.mouseClicked(valueClick,false)&&exact.isEditing(),"clicking the adjacent value opens inline numeric editing");menu.mouseReleased(valueClick);
            InlineInput.field(menu).setValue("5.123456");menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER,0,0));check(Neon.config.zoomFactor==5.123456,"compact inline input keeps exact decimals");
            check(exact.getRight()-sliderCoordinate(exact,"compactTrackRight")>=Ui.width(exact.getMessage().getString())+9,"long exact values reserve enough room beside the slider track");
            check(numbers().get(1).getY()>=exact.getBottom(),"a wrapped exact-value row reflows the following settings immediately");
            menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z,0,GLFW.GLFW_MOD_CONTROL));
            click(button("Zoom"));check(numbers().isEmpty(),"second name click collapses inline settings");
            click(button("Motion blur"));check(numbers().size()==1,"rapid expansion remains responsive during animation");
            check(SoundAudit.neonEnables==quiet+1,"binding and expansion never request enable cues");
            Neon.zoom.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Z));KeyMapping.resetMapping();
            menu.mouseMoved(-20,-20);
        }
        if(age<16)return false;
        Screenshot.grab(mc.gameDirectory,"neon-rows-scale"+(index/2+1)+(index%2==0?"-default":"-minimum")+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});
        var bounds=menu.menuBounds();String capture="neon-rows-window-scale"+(index/2+1)+(index%2==0?"-default":"-minimum")+".png";
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){
            int x=Math.round(bounds.left()*PixelScreen.UI_SCALE),y=Math.round(bounds.top()*PixelScreen.UI_SCALE),w=Math.round(bounds.right()*PixelScreen.UI_SCALE)-x,h=Math.round(bounds.bottom()*PixelScreen.UI_SCALE)-y;
            var crop=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for(int py=0;py<h;py++)for(int px=0;px<w;px++)crop.setRGB(px,py,image.getPixel(x+px,y+py)|0xff000000);
            javax.imageio.ImageIO.write(crop,"png",mc.gameDirectory.toPath().resolve("screenshots").resolve(capture).toFile());
        }catch(Exception failure){throw new AssertionError(failure);}});
        if(++index<6){setup();return false;}
        Neon.config.freelookEnabled=Neon.config.zoomEnabled=false;Neon.freelook.setDown(false);Neon.zoom.setDown(false);
        mc.gui.setScreen(null);age=0;return false;
    }
}

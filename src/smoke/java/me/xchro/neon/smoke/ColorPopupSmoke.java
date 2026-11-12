package me.xchro.neon.smoke;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Real production-client pointer, keyboard, clipboard and viewport checks for the floating picker. */
final class ColorPopupSmoke {
    private final Minecraft mc;
    private final NeonConfig original=ConfigStore.copy(Neon.config);
    private final String clipboard;
    private SettingsScreen menu;
    private int stage=-3, age, checks, sounds, rowY;
    private double scrollBefore;
    ColorPopupSmoke(Minecraft mc,int checks){this.mc=mc;this.checks=checks;clipboard=mc.keyboardHandler.getClipboard();setup(false,1);}
    int checks(){return checks;}
    private void check(boolean ok,String message){if(!ok)throw new AssertionError("Color popup "+stage+": "+message);checks++;System.out.println("NEON_CHECK "+message);}
    private List<AbstractWidget> widgets(ContainerEventHandler root){var all=new ArrayList<AbstractWidget>();for(var c:root.children()){if(c instanceof AbstractWidget w)all.add(w);if(c instanceof ContainerEventHandler group)all.addAll(widgets(group));}return all;}
    private AbstractWidget widget(String label){return widgets(menu).stream().filter(w->w.getMessage().getString().equals(label)).findFirst().orElseThrow();}
    private AbstractWidget popup(){return widget("Color picker");}
    private AbstractScrollArea scroll(){return (AbstractScrollArea)widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiScroll")).reduce((a,b)->b).orElseThrow();}
    private void press(String name){widgets(menu).stream().filter(w->w instanceof Button&&w.getMessage().getString().equals(name)).map(w->(Button)w).findFirst().orElseThrow().onPress(null);}
    private MouseButtonEvent pointer(double x,double y,int button){return new MouseButtonEvent(menu.guiCoordinate(x),menu.guiCoordinate(y),new MouseButtonInfo(button,0));}
    private void click(AbstractWidget w){click(w.getX()+5,w.getY()+7,0);}
    private void click(double x,double y,int button){var e=pointer(x,y,button);menu.mouseClicked(e,false);menu.mouseReleased(e);}
    private void key(int key,int mods){menu.keyPressed(new KeyEvent(key,0,mods));}
    private void undo(){key(GLFW.GLFW_KEY_Z,GLFW.GLFW_MOD_CONTROL);}
    private void next(){stage++;age=0;}
    private void shot(String name){Screenshot.grab(mc.gameDirectory,"neon-popup-"+name+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});}
    private void setup(boolean minimum,int scale){
        new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"),Neon.SAVES);Neon.SAVES.flush().join();
        mc.getWindow().setWindowed(1280,800);mc.options.guiScale().set(scale);mc.resizeGui();mc.setLastInputType(InputType.MOUSE);
        Neon.config=new NeonConfig();mc.gui.setScreen(menu=Menus.fresh(null));
        if(minimum){var b=menu.menuBounds();var e=pointer(b.right()-1,b.bottom()-1,0);menu.mouseClicked(e,false);var end=pointer(b.right()-121,b.bottom()-71,0);menu.mouseDragged(end,end.x()-e.x(),end.y()-e.y());menu.mouseReleased(end);}
        press("Settings");press("Colors");
        scroll().setScrollAmount(150);
    }
    private void fits(){var p=popup();check(p.getX()>=0&&p.getY()>=0&&p.getRight()<=menu.canvasWidth()&&p.getBottom()<=menu.canvasHeight(),"popup and its actions remain on-screen");check(widget("Paste").getBottom()<p.getBottom(),"Copy/Paste remain visible without scrolling");}
    boolean run(){
        if(++age<10)return false;
        switch(stage){
            case -3->{sounds=SoundAudit.neonEnables;press("Appearance");next();}
            case -2->{
                check(widgets(menu).stream().noneMatch(w->Set.of("Theme","Background","Opacity","Reset").contains(w.getMessage().getString())),"Appearance contains no color controls or Reset");
                check(widgets(menu).stream().filter(w->w instanceof ExactSlider).count()==1&&widget("Effects")!=null,"Appearance retains sound and effects");
                press("Colors");scroll().setScrollAmount(0);next();
            }
            case -1->{
                String before=ConfigStore.encode(Neon.config);
                click(widget("Theme"));mc.keyboardHandler.setClipboard("#ED80C2FF");click(widget("Paste"));
                check(Ui.BLUE==0xffed80c2&&Ui.OUTLINE==0xffed80c2&&Neon.config.theme.syncedColors.size()==7,"Theme paste recolors all seven linked roles live");
                check(Neon.config.theme.color(ThemeColor.TEXT)!=Neon.config.theme.color(ThemeColor.BACKGROUND),"Theme keeps text and surfaces distinct");
                undo();check(ConfigStore.encode(Neon.config).equals(before),"one Undo restores the entire prior palette and links");
                key(GLFW.GLFW_KEY_ESCAPE,0); scroll().setScrollAmount(150);next();
            }
            case 0->{rowY=widget("Text").getY();scrollBefore=scroll().scrollAmount();click(widget("Accent"));next();}
            case 1->{
                fits();check((popup().getX()>menu.expandedBounds().right()||popup().getRight()<menu.expandedBounds().left())&&popup().getY()==menu.menuBounds().top(),"popup opens beside the complete menu aligned with its top");
                check(widget("Text").getY()==rowY&&scroll().scrollAmount()==scrollBefore,"opening a popup never moves settings rows or scroll position");shot("default");
                var shade=widget("Saturation and brightness");var start=pointer(shade.getX()+20,shade.getY()+20,0);var end=pointer(shade.getRight()-20,shade.getBottom()-20,0);
                menu.mouseClicked(start,false);menu.mouseDragged(end,end.x()-start.x(),end.y()-start.y());menu.mouseReleased(end);
                check(Neon.config.theme.accent!=0xff84dce6,"color drag applies live");undo();check(Neon.config.theme.accent==0xff84dce6,"one Undo restores an entire color drag");
                var opacity=widgets((ContainerEventHandler)popup()).stream().filter(w->w.getMessage().getString().equals("Opacity")).findFirst().orElseThrow();click(opacity.getX()+opacity.getWidth()/2,opacity.getY()+2,0);check((Neon.config.theme.accent>>>24)<200,"alpha changes live: "+opacity.getClass().getSimpleName()+" at "+opacity.getX()+","+opacity.getY()+" value="+Integer.toHexString(Neon.config.theme.accent));undo();
                mc.keyboardHandler.setClipboard("#33669980");click(widget("Paste"));check(Neon.config.theme.accent==0x80336699,"RGBA paste applies live");click(widget("Copy"));check(mc.keyboardHandler.getClipboard().equals("#33669980"),"Copy retains RGBA byte order");
                mc.keyboardHandler.setClipboard("invalid-color");click(widget("Paste"));check(Neon.config.theme.accent==0x80336699,"invalid paste preserves the current color");undo();check(Neon.config.theme.accent==0xff84dce6,"Undo restores a pasted color");
                click(widget("Sync"));check(Neon.config.theme.syncedColors.contains(ThemeColor.ACCENT),"Sync links an individual color to Theme");
                mc.keyboardHandler.setClipboard("#84DCE680");click(widget("Paste"));check(Neon.config.theme.syncedColors.contains(ThemeColor.ACCENT)&&Neon.config.theme.color(ThemeColor.ACCENT)==0x8084dce6,"alpha-only paste preserves the Theme link");
                mc.keyboardHandler.setClipboard("#33669980");click(widget("Paste"));check(!Neon.config.theme.syncedColors.contains(ThemeColor.ACCENT),"custom RGB editing detaches only that color");
                undo();undo();undo();
                var hex=(EditBox)widget("Hex color");click(hex);hex.setValue("#AA7733CC");key(GLFW.GLFW_KEY_ENTER,0);check(Neon.config.theme.accent==0xccaa7733,"hex editing applies RGBA");undo();check(Neon.config.theme.accent==0xff84dce6,"one Undo restores a completed hex edit");
                click(widget("Outline"));check(widgets(menu).stream().filter(w->w.getMessage().getString().equals("Color picker")).count()==1,"switching color rows keeps exactly one popup");
                key(GLFW.GLFW_KEY_ESCAPE,0);check(widgets(menu).stream().noneMatch(w->w.getMessage().getString().equals("Color picker"))&&mc.gui.screen()==menu,"Escape dismisses only the popup");
                click(widget("Accent"));click(widget("Accent").getX()+5,widget("Accent").getY()+7,1);check(widgets(menu).stream().noneMatch(w->w.getMessage().getString().equals("Color picker")),"right-clicking the selected color closes its popup");
                click(widget("Accent"));click(5,5,0);check(widgets(menu).stream().noneMatch(w->w.getMessage().getString().equals("Color picker")),"outside click dismisses the popup");
                check(SoundAudit.neonEnables==sounds+1,"only the Sync toggle requests an enable cue during color interactions");
                setup(true,2);next();
            }
            case 2->{click(widget("Accent"));next();}
            case 3->{
                fits();check(menu.menuBounds().width()==320,"picker preserves minimum menu dimensions");shot("minimum");
                var p=popup();var start=pointer(p.getX()+20,p.getY()+10,0);var end=pointer(menu.canvasWidth()+100,menu.canvasHeight()+100,0);
                menu.mouseClicked(start,false);menu.mouseDragged(end,end.x()-start.x(),end.y()-start.y());menu.mouseReleased(end);fits();
                mc.setLastInputType(InputType.KEYBOARD_TAB);click(widget("Saturation and brightness"));for(int i=0;i<20&&!widget("Paste").isFocused();i++)key(GLFW.GLFW_KEY_TAB,0);check(widget("Paste").isFocused(),"keyboard reaches all popup actions");
                mc.getWindow().setWindowed(640,480);mc.resizeGui();next();
            }
            case 4->{fits();check(Neon.config.theme.accent!=0,"viewport resize preserves an open, live picker");shot("small-display");setup(false,3);next();}
            case 5->{click(widget("Accent"));next();}
            case 6->{fits();check(popup().getWidth()==184,"GUI scale three retains the same physical popup size");shot("scale3");setupTheme(false);next();}
            case 7->{
                fits();shot("theme-default");
                Neon.save();Neon.SAVES.flush().join();
                try {var saved=new ConfigStore(mc.gameDirectory.toPath().resolve("config/neon.json")).load();check(saved.theme.themeColor==0xffed80c2&&saved.theme.syncedColors.size()==7,"Theme and links survive a real settings file reload");}catch(Exception error){throw new AssertionError(error);}
                key(GLFW.GLFW_KEY_ESCAPE,0);
                var rainbow=widgets(menu).stream().filter(w->w instanceof Button&&w.getMessage().getString().equals("OFF")).map(w->(Button)w).findFirst().orElseThrow();rainbow.onPress(null);
                check(Neon.config.theme.rainbow&&widgets(menu).stream().filter(w->w instanceof ExactSlider).allMatch(w->w.active),"Rainbow activates its inline controls");
                String saved=ConfigStore.encode(Neon.config);Ui.refreshTheme();check(saved.equals(ConfigStore.encode(Neon.config)),"animated theme rendering never mutates saved settings");
                undo();check(!Neon.config.theme.rainbow,"Rainbow toggle supports Undo");
                setupTheme(true);next();
            }
            case 8->{fits();shot("theme-minimum");check(SoundAudit.neonEnables==sounds+2,"Sync and Rainbow each request one enable cue");Neon.applyConfig(original);mc.keyboardHandler.setClipboard(clipboard);mc.gui.setScreen(null);return true;}
        }
        return false;
    }
    private void setupTheme(boolean minimum){setup(minimum,minimum?2:1);scroll().setScrollAmount(0);press("Theme");mc.keyboardHandler.setClipboard("#ED80C2FF");click(widget("Paste"));}
}

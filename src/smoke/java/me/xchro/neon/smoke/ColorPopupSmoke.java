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

/** Real production-client pointer, keyboard, clipboard and viewport checks for the attached color drawer. */
final class ColorPopupSmoke {
    private final Minecraft mc;
    private final NeonConfig original=ConfigStore.copy(Neon.config);
    private final String clipboard;
    private SettingsScreen menu;
    private int stage=-3, age, checks, sounds, rowY;
    private double scrollBefore;
    private String referenceBefore;
    private int feedbackVariant;
    private net.minecraft.client.gui.navigation.ScreenRectangle pasteBounds;
    private CheckerVerification checkerVerification;
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
    private void fits(){var p=popup();var shell=menu.expandedBounds();check(p.getX()>=shell.left()&&p.getY()>=shell.top()&&p.getRight()<shell.right()&&p.getBottom()<shell.bottom(),"color drawer and actions stay inside the shared outline");check(widget("Paste").getBottom()<=p.getBottom(),"Copy/Paste remain visible without scrolling");}
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
                click(widget("Theme"));mc.keyboardHandler.setClipboard("#ED80C2FF");press("Paste");
                check(Ui.BLUE==0xffed80c2&&Ui.OUTLINE==0xffed80c2&&Neon.config.theme.syncedColors.size()==7,"Theme paste recolors all seven linked roles live");
                check(Neon.config.theme.color(ThemeColor.TEXT)!=Neon.config.theme.color(ThemeColor.BACKGROUND),"Theme keeps text and surfaces distinct");
                undo();check(ConfigStore.encode(Neon.config).equals(before),"one Undo restores the entire prior palette and links");
                key(GLFW.GLFW_KEY_ESCAPE,0); scroll().setScrollAmount(150);next();
            }
            case 0->{rowY=widget("Text").getY();scrollBefore=scroll().scrollAmount();click(widget("Accent"));next();}
            case 1->{
                fits();check(popup().getX()>menu.moduleSettingsBounds().right()&&popup().getY()==menu.moduleSettingsBounds().top(),"color editor expands beside settings inside the same shell");
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
                var p=popup();int beforeX=p.getX(),beforeY=p.getY();var start=pointer(p.getX()+20,menu.menuBounds().top()+18,0);var end=pointer(p.getX(),menu.menuBounds().top()+38,0);
                menu.mouseClicked(start,false);menu.mouseDragged(end,end.x()-start.x(),end.y()-start.y());menu.mouseReleased(end);fits();
                check(p.getX()==beforeX-20&&p.getY()==beforeY+20,"color header moves the complete attached menu");
                mc.setLastInputType(InputType.KEYBOARD_TAB);click(widget("Saturation and brightness"));for(int i=0;i<20&&!widget("Paste").isFocused();i++)key(GLFW.GLFW_KEY_TAB,0);check(widget("Paste").isFocused(),"keyboard reaches all popup actions");
                mc.getWindow().setWindowed(640,480);mc.resizeGui();next();
            }
            case 4->{fits();check(Neon.config.theme.accent!=0,"viewport resize preserves an open, live picker");check(widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiScroll")).count()==1,"narrow display replaces details without hidden input targets");shot("small-display");key(GLFW.GLFW_KEY_ESCAPE,0);stage=9;age=0;}
            case 9->{check(widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiScroll")).count()==2,"closing the narrow color drawer restores settings input");setup(false,3);stage=5;age=0;}
            case 5->{click(widget("Accent"));next();}
            case 6->{fits();check(popup().getWidth()==144,"GUI scale three retains the compact physical drawer size");shot("scale3");setupTheme(false);next();}
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
            case 8->{fits();shot("theme-minimum");check(SoundAudit.neonEnables==sounds+2,"Sync and Rainbow each request one enable cue");setupChat(false);stage=10;age=0;}
            case 10,12->{click(widget("Mention color"));next();}
            case 11->{
                fits();shot("mention-default");int before=Neon.config.betterChat.mentionColor;
                mc.keyboardHandler.setClipboard("#CC88EEFF");click(widget("Paste"));check(Neon.config.betterChat.mentionColor==0xffcc88ee,"mention color applies live in the attached drawer");
                undo();check(Neon.config.betterChat.mentionColor==before,"mention color Undo preserves the open drawer");
                key(GLFW.GLFW_KEY_ESCAPE,0);setupChat(true);next();
            }
            case 13->{
                fits();shot("mention-minimum");
                if(java.nio.file.Files.exists(mc.gameDirectory.toPath().resolve("neon-color-reference.json"))){setupReference(0);next();}
                else feedbackSetup();
            }
            case 14,16,18,20->{click(widget(stage<18?"Mention color":"Accent"));next();}
            case 15,17,19,21->{
                fits();shot("vanilla-"+(stage<18?"mention":"settings")+(stage==15||stage==19?"-default":"-minimum"));
                check(ConfigStore.encode(Neon.config).equals(referenceBefore),"opening the drawer preserves the supplied Vanilla palette, alpha, links and mention color");
                if(stage==21){feedbackSetup();return false;}
                setupReference((stage-13)/2);next();
            }
            case 22->{click(widget("Accent"));next();}
            case 23->{
                fits();referenceBefore=ConfigStore.encode(Neon.config);
                var paste=(UiButton)widget("Paste");pasteBounds=paste.getRectangle();
                int cues=SoundAudit.cues.size();mc.keyboardHandler.setClipboard("not a color");click(paste);
                check(ConfigStore.encode(Neon.config).equals(referenceBefore),"rejected paste preserves all settings");
                check(SoundAudit.cues.size()==cues,"rejected paste stays silent");
                check(paste.getRectangle().equals(pasteBounds)&&paste.getMessage().getString().equals("Paste"),"failure feedback preserves Paste geometry and action label");
                check(paste.hint().startsWith("No valid color found.")&&paste.hint().contains("#RRGGBBAA"),"paste failure provides an explanation and accepted formats for hover, focus and narration");
                check("Invalid".equals(status(paste)),"paste failure has visible text as well as a red border");
                next();
            }
            case 24->{
                var paste=(UiButton)widget("Paste");shot("invalid-paste-"+feedbackVariant);
                check("Invalid".equals(status(paste)),"failure feedback survives rendered frames");
                mc.keyboardHandler.setClipboard("#84DCE6FF");click(paste);
                check(status(paste)==null&&!paste.hint().startsWith("No valid"),"valid unchanged paste clears the error");
                paste.setFocused(true);mc.setLastInputType(InputType.KEYBOARD_TAB);mc.keyboardHandler.setClipboard("#33669980");
                paste.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER,0,0));
                check(Neon.config.theme.accent==0x80336699,"keyboard Paste retains RGBA parsing after failure");
                undo();check(ConfigStore.encode(Neon.config).equals(referenceBefore),"invalid and unchanged pastes add no Undo entry");
                if(++feedbackVariant==3){
                    Neon.config.theme.rainbow=true;Neon.config.theme.syncedColors.add(ThemeColor.ACCENT);
                    mc.keyboardHandler.setClipboard("invalid-color");click(paste);stage=26;age=0;
                }
                else feedbackSetup();
            }
            case 25->{if(checkerVerification.run()){checks=checkerVerification.checks();return finish();}}
            case 26->{
                check("Invalid".equals(status((UiButton)widget("Paste"))),"animated theme colors never erase a paste failure");
                Neon.config.theme.rainbow=false;checkerVerification=new CheckerVerification(mc,checks);stage=25;age=0;
            }
        }
        return false;
    }
    private String status(UiButton button){
        try{var field=UiButton.class.getDeclaredField("statusLabel");field.setAccessible(true);return (String)field.get(button);}
        catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    private void feedbackSetup(){
        setup(feedbackVariant>0,feedbackVariant+1);
        if(feedbackVariant==2){mc.getWindow().setWindowed(640,480);mc.resizeGui();}
        stage=22;age=0;
    }
    private void setupTheme(boolean minimum){setup(minimum,minimum?2:1);scroll().setScrollAmount(0);press("Theme");mc.keyboardHandler.setClipboard("#ED80C2FF");press("Paste");}
    private void setupChat(boolean minimum){setup(minimum,minimum?2:1);press("Visuals");Menus.configure(menu,"Better chat");scroll().setScrollAmount(scroll().maxScrollAmount());}
    private boolean finish(){Neon.applyConfig(original);mc.keyboardHandler.setClipboard(clipboard);mc.gui.setScreen(null);return true;}
    private void setupReference(int index){
        setup(index%2==1,index%2+1);
        try{
            var reference=ConfigStore.decode(java.nio.file.Files.readString(mc.gameDirectory.toPath().resolve("neon-color-reference.json")));
            Neon.config.theme=reference.theme;Neon.config.betterChat.mentionColor=reference.betterChat.mentionColor;
        }catch(java.io.IOException error){throw new AssertionError(error);}
        mc.gui.setScreen(menu=Menus.fresh(null));
        if(index<2){Menus.configure(menu,"Better chat");scroll().setScrollAmount(scroll().maxScrollAmount());}
        else {press("Settings");press("Colors");scroll().setScrollAmount(150);}
        referenceBefore=ConfigStore.encode(Neon.config);
    }
}

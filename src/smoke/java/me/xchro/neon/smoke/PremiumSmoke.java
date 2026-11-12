package me.xchro.neon.smoke;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.MenuPlacement;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.*;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Packaged-client checks for the premium interaction pass; all audio is intercepted. */
public final class PremiumSmoke {
    private static PremiumSmoke active;
    private final Minecraft mc;
    private SettingsScreen menu;
    private int checks, variant, age, stage, cues;
    private boolean before;
    private MouseButtonEvent down;
    private String profileName;
    private int actionWidth;
    private final String clipboard;
    private me.xchro.neon.config.ProfileStore originalProfiles;
    private me.xchro.neon.config.QueuedSave testWriter;
    private boolean rejectWrite;
    PremiumSmoke(Minecraft mc,int checks) { this.mc=mc;this.checks=checks;clipboard=mc.keyboardHandler.getClipboard();active=this;setup(); }
    int checks() { return checks; }
    private void check(boolean ok,String message) { if(!ok)throw new AssertionError("Premium "+variant+"/"+stage+": "+message);checks++;System.out.println("NEON_CHECK "+message); }
    private List<AbstractWidget> widgets(ContainerEventHandler root) { var all=new ArrayList<AbstractWidget>();for(var child:root.children()){if(child instanceof AbstractWidget w)all.add(w);if(child instanceof ContainerEventHandler c)all.addAll(widgets(c));}return all; }
    private Button button(String name) { return widgets(menu).stream().filter(w->w instanceof Button&&w.getMessage().getString().equals(name)).map(w->(Button)w).findFirst().orElseThrow(); }
    private AbstractWidget tile() { return widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiModuleTile")&&w.getMessage().getString().equals("Freelook")).findFirst().orElseThrow(); }
    public static int[] pointer() { if(active==null)return null;var w=active.widgets(active.menu).stream().filter(item->item.getClass().getSimpleName().equals("UiModuleTile")&&item.getMessage().getString().equals("Freelook")).findFirst();return w.isPresent()?new int[]{w.get().getX()+12,w.get().getY()+12}:new int[]{-10000,-10000}; }
    private Object field(Object object,String name) throws Exception { Class<?> type=object instanceof Class<?> c?c:object.getClass();while(type!=null){try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(object instanceof Class<?>?null:object);}catch(NoSuchFieldException ignored){type=type.getSuperclass();}}throw new NoSuchFieldException(name); }
    private MouseButtonEvent mouse(double x,double y,int button) { return new MouseButtonEvent(menu.guiCoordinate(x),menu.guiCoordinate(y),new MouseButtonInfo(button,0)); }
    private double pressed(Button button) throws Exception { var m=UiButton.class.getDeclaredMethod("pressFraction");m.setAccessible(true);return (double)m.invoke(button); }
    private me.xchro.neon.core.ActionFeedback acknowledgement(Button button) throws Exception {
        var identity=UiButton.class.getDeclaredMethod("identity");identity.setAccessible(true);
        var feedback=PixelScreen.class.getDeclaredMethod("acknowledgement",String.class);feedback.setAccessible(true);
        return (me.xchro.neon.core.ActionFeedback)feedback.invoke(menu,identity.invoke(button));
    }
    private void revealProfileForm() throws Exception { var scroll=(AbstractScrollArea)field(menu,"details");scroll.setScrollAmount(scroll.maxScrollAmount()); }
    private void requestSave(String name) throws Exception {
        var method=SettingsScreen.class.getDeclaredMethod("saveProfile",String.class,boolean.class);method.setAccessible(true);method.invoke(menu,name,true);
    }
    private Object cursor(PixelScreen screen,double x,double y) throws Exception {
        var g=new net.minecraft.client.gui.GuiGraphicsExtractor(mc,new net.minecraft.client.renderer.state.gui.GuiRenderState(),-10000,-10000);
        var method=PixelScreen.class.getDeclaredMethod("cursor",net.minecraft.client.gui.GuiGraphicsExtractor.class,int.class,int.class);method.setAccessible(true);method.invoke(screen,g,(int)x,(int)y);
        return field(g,"pendingCursor");
    }
    private void windowCursors() throws Exception {
        var b=menu.expandedBounds();var move=com.mojang.blaze3d.platform.cursor.CursorTypes.RESIZE_ALL;
        check(cursor(menu,b.left()+20,b.top()+12)==move,"header advertises direct movement");
        Object corner=cursor(menu,b.left(),b.top());check(corner.toString().contains("northwest")||corner==move,"invisible corner advertises diagonal resize");
        down=mouse(b.left(),b.top(),0);menu.mouseClicked(down,false);check(cursor(menu,-100,-100)==corner,"resize cursor persists outside during gesture");menu.mouseReleased(mouse(-100,-100,0));
        check(cursor(menu,-100,-100)==com.mojang.blaze3d.platform.cursor.CursorType.DEFAULT,"release restores ordinary cursor outside");
        Menus.configure(menu,"Freelook");
    }
    private void sliderAndHudCursors() throws Exception {
        var slider=widgets(menu).stream().filter(w->w instanceof ExactSlider).map(w->(ExactSlider)w).findFirst().orElseThrow();
        var xMethod=ExactSlider.class.getDeclaredMethod("compactTrackLeft");var yMethod=ExactSlider.class.getDeclaredMethod("compactTrackY");xMethod.setAccessible(true);yMethod.setAccessible(true);
        int x=(int)xMethod.invoke(slider)+5,y=(int)yMethod.invoke(slider);
        var g=new net.minecraft.client.gui.GuiGraphicsExtractor(mc,new net.minecraft.client.renderer.state.gui.GuiRenderState(),-10000,-10000);g.pose().scale(PixelScreen.UI_SCALE/menu.pixelScale());
        slider.extractRenderState(g,x,y,0);check(field(g,"pendingCursor")==com.mojang.blaze3d.platform.cursor.CursorTypes.RESIZE_EW,"slider track advertises adjustment");
        down=mouse(x,y,0);menu.mouseClicked(down,false);check(cursor(menu,-100,-100)==com.mojang.blaze3d.platform.cursor.CursorTypes.RESIZE_EW,"slider keeps adjustment cursor outside");menu.mouseReleased(mouse(-100,-100,0));
        Neon.config.armor.visible=true;Neon.config.armorAnchor=me.xchro.neon.config.NeonConfig.ArmorAnchor.CUSTOM;Neon.config.armor.x=.5;Neon.config.armor.y=.5;
        var editor=new HudEditor(menu);mc.gui.setScreen(editor);editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_H,0,0));
        var element=Class.forName("me.xchro.neon.ui.HudEditor$Element");var boundsMethod=HudEditor.class.getDeclaredMethod("bounds",element);boundsMethod.setAccessible(true);
        var b=(me.xchro.neon.hud.HudBounds)boundsMethod.invoke(editor,element.getEnumConstants()[0]);
        double cx=(b.x()+b.width()/2.0)/PixelScreen.UI_SCALE,cy=(b.y()+b.height()/2.0)/PixelScreen.UI_SCALE;
        check(cursor(editor,cx,cy)==com.mojang.blaze3d.platform.cursor.CursorTypes.RESIZE_ALL,"enabled HUD widget advertises movement");
        Neon.config.armor.visible=false;check(cursor(editor,cx,cy)==com.mojang.blaze3d.platform.cursor.CursorType.DEFAULT,"disabled HUD widgets have no cursor target");
        editor.onClose();
    }
    private void setup() {
        mc.options.enableVsync().set(false);mc.options.framerateLimit().set(new int[]{30,60,144}[variant/2]);mc.options.guiScale().set(variant/2+1);
        mc.getWindow().setWindowed(variant<4?1280:640,variant<4?800:480);mc.resizeGui();mc.setLastInputType(InputType.MOUSE);
        new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"),Neon.SAVES);Neon.SAVES.flush().join();
        mc.gui.setScreen(menu=Menus.fresh(null));
        if(variant%2==1){var b=menu.menuBounds();var p=mouse(b.right()-1,b.bottom()-1,0);var q=mouse(b.right()-121,b.bottom()-71,0);menu.mouseClicked(p,false);menu.mouseDragged(q,q.x()-p.x(),q.y()-p.y());menu.mouseReleased(q);}
        age=stage=0;
    }
    boolean run() throws Exception { try{return step();}catch(Exception|Error failure){mc.keyboardHandler.setClipboard(clipboard);throw failure;} }
    private boolean step() throws Exception {
        if(++age<(stage==22?12:8))return false;age=0;
        switch(stage++) {
            case 0 -> { var b=button("Freelook");before=Neon.config.freelookEnabled; cues=SoundAudit.cues.size();down=mouse(b.getX()+12,b.getY()+12,0);menu.mouseClicked(down,false);check(Neon.config.freelookEnabled!=before,"mouse action applies immediately"); }
            case 1 -> { check(pressed(button("Freelook"))>.99,"held press settles at every frame cap");check(SoundAudit.cues.size()==cues+1,"press adds no extra cue");menu.mouseReleased(mouse(-100,-100,0)); }
            case 2 -> { check(pressed(button("Freelook"))==0,"outside release clears feedback");var b=button("Freelook");b.setFocused(true);before=Neon.config.freelookEnabled;b.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER,0,0));check(Neon.config.freelookEnabled!=before,"keyboard action applies immediately"); }
            case 3 -> { check(pressed(button("Freelook"))>.99,"keyboard press has matching feedback");menu.keyReleased(new KeyEvent(GLFW.GLFW_KEY_ENTER,0,0)); }
            case 4 -> { check(pressed(button("Freelook"))==0,"keyboard release clears feedback");var b=button("Freelook");down=mouse(b.getX()+12,b.getY()+12,1);menu.mouseClicked(down,false); }
            case 5 -> { check(pressed(button("Freelook"))>.99,"drawer rebuild retains pressed identity");menu.mouseReleased(mouse(-100,-100,1)); }
            case 6 -> { var bind=widgets((ContainerEventHandler)tile()).stream().filter(w->w.getClass().getSimpleName().equals("UiBindButton")).map(w->(Button)w).findFirst().orElseThrow();before=Neon.config.freelookEnabled;down=mouse(bind.getRight()-2,bind.getY()+10,1);menu.mouseClicked(down,false);check(Neon.config.freelookEnabled==before,"bind press does not toggle module"); }
            case 7 -> { check(pressed(button("Freelook"))==0,"bind feedback stays off module body");menu.mouseReleased(mouse(-100,-100,1));var b=button("Freelook");menu.mouseClicked(mouse(b.getX()+12,b.getY()+12,0),false);LiveSmoke.focused=false;menu.tick();LiveSmoke.focused=true;check(pressed(button("Freelook"))==0,"focus loss clears held feedback"); }
            case 8 -> { Screenshot.grab(mc.gameDirectory,"neon-premium-"+variant+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});button("Settings").onPress(null);check(button("Colors")!=null,"incoming category is interactive immediately");check(!((List<?>)field(field(menu,"presentation"),"outgoing")).isEmpty(),"outgoing category retains drawing commands"); }
            case 9 -> {
                check(((List<?>)field(field(menu,"presentation"),"outgoing")).isEmpty(),"category crossfade finishes without queued pages");button("Visuals").onPress(null);
                var b=button("Freelook");menu.mouseClicked(mouse(b.getX()+12,b.getY()+12,0),false);
                button("Settings").onPress(null);button("Visuals").onPress(null);
                check(pressed(button("Freelook"))==0,"removing a held control clears its press state");
                button("Settings").onPress(null);check(button("Profiles")!=null,"rapid navigation selects latest category");
            }
            case 10 -> {
                var presentation=field(menu,"presentation");var opacity=presentation.getClass().getDeclaredField("backdropOpacity");opacity.setAccessible(true);opacity.setFloat(presentation,.4f);
                menu.onClose();check(mc.gui.screen()==null,"closing releases gameplay immediately");check(field(MenuPresentation.class,"exit")!=null,"closing retains a render-only exit snapshot");
                check((float)field(field(MenuPresentation.class,"exit"),"backdropOpacity")==.4f,"close snapshot retains the opening backdrop opacity");
            }
            case 11 -> { check(field(MenuPresentation.class,"exit")==null,"exit snapshot expires after fade");mc.gui.setScreen(menu=Menus.fresh(null)); }
            case 12 -> { menu.onClose();mc.gui.setScreen(menu=Menus.fresh(null));check(field(MenuPresentation.class,"exit")==null,"reopening clears stale exit presentation");Ui.releaseFonts();check(field(MenuPresentation.class,"exit")==null,"resource invalidation clears presentation"); }
            case 13 -> {
                menu.onClose();check(field(MenuPresentation.class,"exit")!=null,"resource test starts with a closing presentation");
                Ui.releaseFonts();check(field(MenuPresentation.class,"exit")==null,"resource reload discards live exit commands");
                mc.gui.setScreen(menu=Menus.fresh(null));windowCursors();
            }
            case 14 -> sliderAndHudCursors();
            case 15 -> { button("Settings").onPress(null);Menus.configure(menu,"Colors"); }
            case 16 -> Menus.configure(menu,"Outline");
            case 17 -> { mc.keyboardHandler.setClipboard("premium verification");var copy=button("Copy");actionWidth=copy.getWidth();cues=SoundAudit.cues.size();copy.onPress(null);check(acknowledgement(copy)!=null&&acknowledgement(copy).label().equals("Copied"),"copy confirms after clipboard write");check(copy.getWidth()==actionWidth&&copy.getMessage().getString().equals("Copy"),"copy feedback preserves geometry and semantic label");check(SoundAudit.cues.size()==cues+1,"copy confirmation emits one cue"); }
            case 18 -> { check(acknowledgement(button("Copy"))!=null,"copy acknowledgement remains visible for 900 ms");Screenshot.grab(mc.gameDirectory,"neon-premium-copy-"+variant+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE,0,0));Menus.configure(menu,"Profiles"); }
            case 19 -> { profileName="Premium "+variant+" "+(System.nanoTime()%1000000);var edit=widgets(menu).stream().filter(w->w instanceof EditBox&&w.getMessage().getString().equals("Profile name")).map(w->(EditBox)w).findFirst().orElseThrow();edit.setValue(profileName);revealProfileForm();actionWidth=button("Save").getWidth();cues=SoundAudit.cues.size();button("Save").onPress(null);check(acknowledgement(button("Save"))==null,"profile does not claim success before queued write completes");revealProfileForm(); }
            case 20 -> { var save=button("Save");check(acknowledgement(save)!=null&&acknowledgement(save).label().equals("Saved"),"durable profile write confirms through rebuild");check(save.getWidth()==actionWidth&&save.getMessage().getString().equals("Save"),"saved feedback keeps button geometry and semantic label");check(SoundAudit.cues.size()==cues+1,"asynchronous save emits exactly one confirmation");Screenshot.grab(mc.gameDirectory,"neon-premium-saved-"+variant+".png",mc.gameRenderer.mainRenderTarget(),1,message->{}); }
            case 21 -> { }
            case 22 -> { check(acknowledgement(button("Save"))==null,"saved acknowledgement expires");Neon.config.freelookDistance=Neon.config.freelookDistance==5?6:5;var update=widgets(menu).stream().filter(w->w instanceof Button&&w.getMessage().getString().equals("Update")).map(w->(Button)w).toList().getLast();update.onPress(null);var yes=widgets(mc.gui.screen()).stream().filter(w->w instanceof Button&&w.getMessage().getString().equals("Yes")).map(w->(Button)w).findFirst().orElseThrow();yes.onPress(null); }
            case 23 -> { var update=widgets(menu).stream().filter(w->w instanceof Button&&w.getMessage().getString().equals("Update")).map(w->(Button)w).toList().getLast();check(acknowledgement(update)!=null&&acknowledgement(update).label().equals("Saved"),"profile update confirms only after persistence");button("Visuals").onPress(null);check(((Map<?,?>)field(menu,"acknowledgements")).isEmpty(),"navigation clears local feedback"); }
            case 24 -> {
                if(++variant<6){setup();return false;}
                originalProfiles=Neon.profiles;
                testWriter=new me.xchro.neon.config.QueuedSave((path,json)->{if(rejectWrite)throw new java.io.IOException("Expected verification failure");},(path,failure)->{});
                Neon.profiles=new me.xchro.neon.config.ProfileStore(mc.gameDirectory.toPath().resolve("premium-failure-fixture.json"),testWriter);
                Neon.profiles.initialize(Neon.config);testWriter.flush().join();
                button("Settings").onPress(null);cues=SoundAudit.cues.size();rejectWrite=true;requestSave("Expected failure");
            }
            case 25 -> {
                check(acknowledgement(button("Save"))==null,"failed persistence never displays Saved");
                check(field(menu,"error").equals("Could not save profile."),"failed persistence retains local error handling");
                check(SoundAudit.cues.size()==cues,"failed persistence emits no confirmation");
                rejectWrite=false;requestSave("Obsolete after navigation");button("Visuals").onPress(null);cues=SoundAudit.cues.size();
            }
            case 26 -> {
                check(((Map<?,?>)field(menu,"acknowledgements")).isEmpty()&&SoundAudit.cues.size()==cues,"navigation ignores obsolete asynchronous completion");
                button("Settings").onPress(null);cues=SoundAudit.cues.size();requestSave("Older request");requestSave("Newer request");
            }
            case 27 -> {
                check(acknowledgement(button("Save"))!=null&&SoundAudit.cues.size()==cues+1,"newest operation alone produces Saved and one cue");
                testWriter.close();Neon.profiles=originalProfiles;
            }
            case 28 -> {
                menu.onClose();check(field(MenuPresentation.class,"exit")!=null,"resize test starts with a closing presentation");
                mc.getWindow().setWindowed(641,480);mc.resizeGui();MenuPresentation.exitBlur();
                check(field(MenuPresentation.class,"exit")==null,"viewport change discards stale exit commands");mc.gui.setScreen(menu=Menus.fresh(null));
            }
            case 29 -> {
                menu.onClose();mc.gui.setScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("Unrelated verification screen")){});
                MenuPresentation.exitBlur();check(field(MenuPresentation.class,"exit")==null,"unrelated screens discard stale exit commands");
                active=null;mc.keyboardHandler.setClipboard(clipboard);mc.gui.setScreen(null);return true;
            }
        }
        return false;
    }
}

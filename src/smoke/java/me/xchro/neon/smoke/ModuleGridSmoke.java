package me.xchro.neon.smoke;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.core.BindMode;
import me.xchro.neon.module.ClientModule;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/** Pointer-level acceptance and per-frame GPU perimeter checks against the runnable JAR. */
public final class ModuleGridSmoke {
    private static ModuleGridSmoke active;
    private final Minecraft mc;
    private final NeonConfig original=ConfigStore.copy(Neon.config);
    private final Map<ClientModule,String> keys=new EnumMap<>(ClientModule.class);
    private final String menuKey=Neon.settings.saveString(),alternateMenuKey=Neon.settingsAlternate.saveString();
    private SettingsScreen menu;
    private int stage,age,checks,frames,perimeterPixels,hudCase;
    private static final String[][] HUD_VIEWS={{"Visuals","Better chat"},{"Combat","Autoclicker"},{"Settings","Colors"}};
    private boolean audit,pending,hoverBinding;
    private int[] border;
    private volatile String failure;
    ModuleGridSmoke(Minecraft mc,int checks) {
        this.mc=mc;this.checks=checks;active=this;
        for(var module:ClientModule.values())keys.put(module,module.key().saveString());
        mc.options.framerateLimit().set(120);mc.options.enableVsync().set(false);
        setup(false,1,1280,800);
    }
    int checks(){return checks;}
    private void check(boolean ok,String text){if(!ok)throw new AssertionError("Grid "+stage+": "+text);checks++;System.out.println("NEON_CHECK "+text);}
    private List<AbstractWidget> widgets(ContainerEventHandler root){var list=new ArrayList<AbstractWidget>();for(var child:root.children()){if(child instanceof AbstractWidget w)list.add(w);if(child instanceof ContainerEventHandler c)list.addAll(widgets(c));}return list;}
    private AbstractWidget tile(String label){return widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiModuleTile")&&w.getMessage().getString().equals(label)).findFirst().orElseThrow();}
    private AbstractWidget bind(String label){return widgets((ContainerEventHandler)tile(label)).stream().filter(w->w.getClass().getSimpleName().equals("UiBindButton")).findFirst().orElseThrow();}
    public static int[] pointer(){
        if(active==null)return null;
        if(!active.hoverBinding)return new int[]{-10000,-10000};
        var bind=active.bind("Freelook");return new int[]{bind.getX()+bind.getWidth()/2,bind.getY()+12};
    }
    private Button button(String label){return widgets(menu).stream().filter(w->w instanceof Button&&w.getMessage().getString().equals(label)).map(w->(Button)w).findFirst().orElseThrow();}
    private void press(String label){button(label).onPress(null);}
    private MouseButtonEvent pointer(double x,double y,int b){return new MouseButtonEvent(menu.guiCoordinate(x),menu.guiCoordinate(y),new MouseButtonInfo(b,0));}
    private void click(double x,double y,int b){var e=pointer(x,y,b);check(menu.mouseClicked(e,false),"pointer accepted");menu.mouseReleased(e);}
    private void body(String label,int b){var w=tile(label);click(w.getX()+5,w.getY()+12,b);}
    private void binding(String label,int b){var w=bind(label);click(w.getX()+3,w.getY()+12,b);}
    private void key(int key,int mods){menu.keyPressed(new KeyEvent(key,0,mods));}
    private AbstractScrollArea drawer(){return (AbstractScrollArea)widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiScroll")).reduce((a,b)->b).orElseThrow();}
    private void next(){stage++;age=0;}
    private void shot(String name){Screenshot.grab(mc.gameDirectory,"neon-grid-"+name+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});}
    private void dragHeader(int dx){
        var b=menu.menuBounds();var from=pointer(b.left()+30,b.top()+18,0);var to=pointer(b.left()+30+dx,b.top()+18,0);
        menu.mouseClicked(from,false);menu.mouseDragged(to,to.x()-from.x(),0);menu.mouseReleased(to);
    }
    private void setup(boolean minimum,int scale,int width,int height){
        audit=false;
        new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"),Neon.SAVES);Neon.SAVES.flush().join();
        mc.getWindow().setWindowed(width,height);mc.options.guiScale().set(scale);mc.resizeGui();mc.setLastInputType(InputType.MOUSE);
        Neon.config=new NeonConfig();mc.gui.setScreen(menu=Menus.fresh(null));
        if(minimum){var b=menu.menuBounds();var e=pointer(b.right()-1,b.bottom()-1,0);menu.mouseClicked(e,false);var end=pointer(b.right()-121,b.bottom()-71,0);menu.mouseDragged(end,0,0);menu.mouseReleased(end);}
    }
    private void fits(){
        var shell=menu.expandedBounds();check(shell.left()>=13&&shell.right()<=menu.canvasWidth()-13,"animated shell stays within display");
        var tiles=widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiModuleTile")).toList();
        check(tiles.stream().map(AbstractWidget::getWidth).distinct().count()==1,"all buttons have exactly equal widths");
        for(var t:tiles){check(t.getHeight()==26,"button and bind stay on a single compact line");check(t.getRight()<=menu.menuBounds().right()-8,"button fits grid");for(var b:widgets((ContainerEventHandler)t))check(b.getX()>=t.getX()&&b.getRight()<=t.getRight(),"button contents fit");}
        var detail=widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiScroll")).skip(1).findFirst();
        if(detail.isPresent())for(var w:widgets((ContainerEventHandler)detail.get()))check(w.getX()>=detail.get().getX()&&w.getRight()<=detail.get().getRight(),"drawer control fits its viewport");
    }
    boolean run(){
        if(failure!=null)throw new AssertionError(failure);
        age++;
        if(stage==2){
            if(age%3==0&&age<=30)body(new String[]{"Zoom","Zoom","Freelook","Freelook","Zoom"}[(age/3-1)%5],1);
            if(age==34){body("Zoom",1);next();}
            return false;
        }
        if(age<12)return false;
        switch(stage){
            case 0->{
                fits();shot("default-collapsed");
                boolean before=Neon.config.freelookEnabled;body("Freelook",0);
                check(Neon.config.freelookEnabled!=before,"left click toggles module");
                check(menu.expandedBounds().width()==menu.menuBounds().width(),"toggle does not open settings");
                key(GLFW.GLFW_KEY_Z,GLFW.GLFW_MOD_CONTROL);check(Neon.config.freelookEnabled==before,"Undo restores module");
                for(var expected:new BindMode[]{BindMode.REVERSE_HOLD,BindMode.TOGGLE,BindMode.HOLD}){binding("Freelook",1);check(Neon.config.freelookMode==expected,"right click bind selects "+expected);check(Neon.config.freelookEnabled==before,"bind mode never toggles module");}
                binding("Freelook",0);click(bind("Freelook").getX()+3,bind("Freelook").getY()+12,4);
                check(Neon.freelook.saveString().equals("key.mouse.5"),"bracket bind captures Mouse 5");
                next();
            }
            case 1->{fits();check(bind("Freelook").getMessage().getString().equals("[M5]"),"assigned bind stays bracketed on button");body("Zoom",1);audit=true;next();}
            case 3->{
                if(widgets(menu).stream().noneMatch(w->w instanceof ExactSlider)){body("Zoom",1);age=0;return false;}
                fits();shot("default-expanded");
                check(widgets(menu).stream().noneMatch(w->Set.of("Activation","Toggle").contains(w.getMessage().getString())),"activation controls absent from settings");
                check(frames>=10&&perimeterPixels>10000,"continuous native-pixel outline across "+frames+" rendered animation frames and "+perimeterPixels+" perimeter pixels");
                dragHeader(400);int clamped=menu.menuBounds().left();dragHeader(-20);
                check(menu.menuBounds().left()==clamped-20,"expanded header drag starts at its visible position after edge clamping");
                setup(true,2,1280,800);press("Combat");body("Autoclicker",1);audit=true;next();
            }
            case 4->{fits();shot("minimum-expanded");setup(false,3,640,480);body("Zoom",1);audit=true;next();}
            case 5->{
                fits();shot("small-display-expanded");
                key(GLFW.GLFW_KEY_ESCAPE,0);next();
            }
            case 6->{
                check(menu.expandedBounds().width()==menu.menuBounds().width(),"Escape closes drawer fully");
                setup(false,1,1280,800);press("Settings");
                String before=ConfigStore.encode(Neon.config);body("Colors",0);
                check(ConfigStore.encode(Neon.config).equals(before),"left click opens a Settings section without changing configuration");
                next();
            }
            case 7->{
                fits();shot("settings-default");
                check(widgets(menu).stream().noneMatch(w->w.getClass().getSimpleName().equals("UiBindButton")),"Settings buttons have no bindings");
                drawer().setScrollAmount(90);body("Appearance",1);next();
            }
            case 8->{
                fits();check(widgets(menu).stream().anyMatch(w->w.getMessage().getString().equals("Effects")),"right click opens Appearance in the drawer");
                body("Colors",0);next();
            }
            case 9->{
                check(drawer().scrollAmount()==90,"returning to Colors restores its drawer scroll position");
                menu.onClose();mc.gui.setScreen(menu=new SettingsScreen(null));next();
            }
            case 10->{
                check(drawer().scrollAmount()==90,"reopening the menu restores Settings and its drawer scroll position");
                setup(true,2,1280,800);press("Settings");body("Profiles",0);next();
            }
            case 11->{
                fits();drawer().setScrollAmount(drawer().maxScrollAmount());shot("settings-profiles-minimum");
                var name=(EditBox)widgets(menu).stream().filter(w->w instanceof EditBox).findFirst().orElseThrow();
                name.setValue("Drawer verification");press("Save");
                check(Neon.profiles.names().contains("Drawer verification"),"profile name and Save work inside the minimum drawer");
                Neon.config.theme.soundVolume=.17;press("Drawer verification");
                check(Neon.config.theme.soundVolume==.4,"profile loads from the new drawer");
                key(GLFW.GLFW_KEY_Z,GLFW.GLFW_MOD_CONTROL);
                check(Neon.config.theme.soundVolume==.17,"profile load remains undoable");
                try { Neon.profiles.delete("Drawer verification"); } catch (Exception e) { throw new AssertionError(e); }
                key(GLFW.GLFW_KEY_ESCAPE,0);next();
            }
            case 12->{
                check(menu.expandedBounds().width()==menu.menuBounds().width(),"Escape closes the Settings drawer and keeps its grid visible");
                setup(false,1,1280,800);hoverBinding=true;next();
            }
            case 13->{fits();shot("bind-hover-hold-default");binding("Freelook",1);next();}
            case 14->{shot("bind-hover-reverse-hold");binding("Freelook",1);next();}
            case 15->{shot("bind-hover-toggle");hoverBinding=false;next();}
            case 16->{shot("bind-hover-leave-default");setup(true,2,1280,800);hoverBinding=true;next();}
            case 17->{fits();shot("bind-hover-hold-minimum");hoverBinding=false;next();}
            case 18->{
                shot("bind-hover-leave-minimum");
                Neon.settings.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_INSERT));
                Neon.settingsAlternate.setKey(Neon.settingsAlternate.getDefaultKey());KeyMapping.resetMapping();
                check(Neon.settingsAlternate.getDefaultKey().getValue()==GLFW.GLFW_KEY_RIGHT_SHIFT,"alternate menu key defaults to Right Shift");
                key(GLFW.GLFW_KEY_INSERT,0);check(mc.gui.screen()==null,"Insert closes the menu");
                KeyMapping.click(Neon.settingsAlternate.getDefaultKey());next();
            }
            case 19->{
                check(mc.gui.screen() instanceof SettingsScreen,"Right Shift opens the menu alongside Insert");menu=(SettingsScreen)mc.gui.screen();
                key(GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_MOD_SHIFT);check(mc.gui.screen()==null,"Right Shift closes the menu");
                KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_INSERT));next();
            }
            case 20->{
                check(mc.gui.screen() instanceof SettingsScreen,"Insert still opens the menu with Right Shift registered");menu=(SettingsScreen)mc.gui.screen();
                binding("Freelook",0);key(GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_MOD_SHIFT);
                check(mc.gui.screen()==menu&&Neon.freelook.matches(new KeyEvent(GLFW.GLFW_KEY_RIGHT_SHIFT,0,GLFW.GLFW_MOD_SHIFT)),"bind capture accepts Right Shift without closing the menu");
                key(GLFW.GLFW_KEY_Z,GLFW.GLFW_MOD_CONTROL);
                press("Settings");body("Profiles",0);
                var field=(EditBox)widgets(menu).stream().filter(w->w instanceof EditBox).findFirst().orElseThrow();
                focus(menu,field);key(GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_MOD_SHIFT);
                check(mc.gui.screen()==menu&&field.isFocused(),"Right Shift preserves focused profile text entry");
                mc.gui.setScreen(new net.minecraft.client.gui.screens.ChatScreen("unsent menu shortcut check",false));
                KeyMapping.click(Neon.settingsAlternate.getDefaultKey());KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_INSERT));next();
            }
            case 21->{
                check(mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen,"both menu shortcuts leave chat open");mc.gui.setScreen(null);next();
            }
            case 22->{
                check(mc.gui.screen()==null,"shortcuts pressed in chat do not leak into gameplay");
                KeyMapping.click(Neon.settingsAlternate.getDefaultKey());KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_INSERT));next();
            }
            case 23->{
                check(mc.gui.screen() instanceof SettingsScreen,"simultaneous menu shortcuts open once");menu=(SettingsScreen)mc.gui.screen();
                key(GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_MOD_SHIFT);check(mc.gui.screen()==null,"Right Shift closes after simultaneous shortcuts");next();
            }
            case 24->{
                check(mc.gui.screen()==null,"both menu key queues are drained after closing");
                next();
            }
            case 25->{
                setup(hudCase>=3,hudCase%3+1,1280,800);
                Neon.config.theme.alphaEnabled=true;
                Neon.config.theme.background=0x7807090e;Neon.config.theme.cards=0xd90d1119;
                var view=HUD_VIEWS[hudCase%3];press(view[0]);
                var grid=(AbstractScrollArea)widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiScroll")).findFirst().orElseThrow();
                grid.setScrollAmount(Math.max(0,grid.scrollAmount()+tile(view[1]).getBottom()-grid.getBottom()));
                body(view[1],1);next();
            }
            case 26->{
                var view=HUD_VIEWS[hudCase%3];var launcher=button("HUD editor");
                var grid=(AbstractScrollArea)widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiScroll")).findFirst().orElseThrow();
                grid.setScrollAmount(grid.maxScrollAmount());drawer().setScrollAmount(drawer().maxScrollAmount());
                double gridPosition=grid.scrollAmount(),drawerPosition=drawer().scrollAmount();
                String config=ConfigStore.encode(Neon.config);
                shot("sidebar-"+view[0].toLowerCase()+"-"+(hudCase>=3?"minimum":"default"));
                if(hudCase%3==0)click(launcher.getX()+launcher.getWidth()/2,launcher.getY()+launcher.getHeight()/2,0);
                else {mc.setLastInputType(InputType.KEYBOARD_TAB);focus(menu,launcher);key(hudCase%3==1?GLFW.GLFW_KEY_ENTER:GLFW.GLFW_KEY_SPACE,0);}
                check(mc.gui.screen() instanceof HudEditor,"sidebar action opens HUD directly from "+view[0]);
                mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE,0,0));
                check(mc.gui.screen()==menu,"Escape returns to the originating "+view[0]+" menu");
                check(widgets(menu).stream().anyMatch(w->w.getMessage().getString().equals(view[1]))&&widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiScroll")).count()==2,"return preserves the category and expanded drawer");
                var restoredGrid=(AbstractScrollArea)widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiScroll")).findFirst().orElseThrow();
                check(restoredGrid.scrollAmount()==gridPosition&&drawer().scrollAmount()==drawerPosition,"HUD round trip preserves both scroll positions");
                check(ConfigStore.encode(Neon.config).equals(config),"opening HUD preserves saved theme alpha and settings");
                if(++hudCase<6){stage=25;age=0;}else next();
            }
            case 27->{
                check(SoundAudit.vanillaClicks==0,"all verification interactions avoid vanilla button sounds");
                audit=false;active=null;
                Neon.settings.setKey(InputConstants.getKey(menuKey));Neon.settingsAlternate.setKey(InputConstants.getKey(alternateMenuKey));
                for(var entry:keys.entrySet())entry.getKey().key().setKey(InputConstants.getKey(entry.getValue()));KeyMapping.resetMapping();Neon.applyConfig(original);
                mc.gui.setScreen(null);return true;
            }
        }
        return false;
    }
    private boolean focus(ContainerEventHandler group,net.minecraft.client.gui.components.events.GuiEventListener target){
        for(var child:group.children())if(child==target||child instanceof ContainerEventHandler nested&&focus(nested,target)){group.setFocused(child);return true;}
        return false;
    }
    public static void border(int x,int y,int w,int h){if(active!=null&&active.audit)active.border=new int[]{x,y,w,h};}
    public static void frameEnd(){
        var a=active;if(a==null||!a.audit||a.pending||a.border==null)return;
        int[] b=a.border.clone();a.pending=true;
        Screenshot.takeScreenshot(a.mc.gameRenderer.mainRenderTarget(),image->{
            try(image){
                int gaps=0,count=0;
                for(int x=b[0];x<b[0]+b[2];x++){if(!bright(image.getPixel(x,b[1])))gaps++;if(!bright(image.getPixel(x,b[1]+b[3]-1)))gaps++;count+=2;}
                for(int y=b[1]+1;y<b[1]+b[3]-1;y++){if(!bright(image.getPixel(b[0],y)))gaps++;if(!bright(image.getPixel(b[0]+b[2]-1,y)))gaps++;count+=2;}
                if(gaps>0)a.failure="Animated outline has "+gaps+" missing perimeter pixels in frame "+a.frames+" bounds="+Arrays.toString(b);
                a.frames++;a.perimeterPixels+=count;
            }catch(Exception e){a.failure="Outline GPU readback: "+e;}finally{a.pending=false;}
        });
    }
    private static boolean bright(int pixel){int r=pixel>>16&255,g=pixel>>8&255,b=pixel&255;return Math.max(r,Math.max(g,b))>60;}
}

package me.xchro.neon.smoke;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.hud.FpsHud;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Typing, keyboard focus, canvas selection and small-window search in the packaged client. */
final class SearchSmoke {
    private final Minecraft mc;
    private final NeonConfig original;
    private SettingsScreen menu;
    private HudEditor editor;
    private int checks,stage,age,variant;
    private double gridPosition,hudPosition;
    SearchSmoke(Minecraft mc,int checks){this.mc=mc;this.checks=checks;original=ConfigStore.copy(Neon.config);}
    int checks(){return checks;}
    private void check(boolean ok,String message){if(!ok)throw new AssertionError("Search "+variant+"/"+stage+": "+message);checks++;System.out.println("NEON_CHECK "+message);}
    private List<AbstractWidget> widgets(ContainerEventHandler parent){var result=new ArrayList<AbstractWidget>();for(var child:parent.children()){if(child instanceof AbstractWidget w)result.add(w);if(child instanceof ContainerEventHandler nested)result.addAll(widgets(nested));}return result;}
    private PixelScreen screen(){return (PixelScreen)mc.gui.screen();}
    private AbstractWidget named(String name){return widgets(screen()).stream().filter(w->w.getMessage().getString().equals(name)).findFirst().orElseThrow();}
    private EditBox search(){return widgets(screen()).stream().filter(w->w instanceof EditBox&&w.getClass().getSimpleName().equals("UiSearch")).map(EditBox.class::cast).findFirst().orElseThrow();}
    private boolean searchHidden(){return widgets(screen()).stream().noneMatch(w->w.getClass().getSimpleName().equals("UiSearch"));}
    private void escapeFromResult(int focusKey, double originalScroll) {
        PixelScreen owner = screen();
        String before = ConfigStore.encode(Neon.config);
        key(focusKey, 0);
        check(!search().isFocused(), "focus moved from search to a result");
        key(GLFW.GLFW_KEY_ESCAPE, 0);
        check(mc.gui.screen() == owner && !searchHidden() && search().getValue().isEmpty(),
            "Escape from a result clears the query in the same screen");
        check(Math.abs(list().scrollAmount() - originalScroll) < 1, "clearing from a result restores scrolling");
        key(GLFW.GLFW_KEY_ESCAPE, 0);
        check(mc.gui.screen() == owner && searchHidden(), "second Escape from results reclaims the row");
        check(ConfigStore.encode(Neon.config).equals(before), "leaving search preserves settings");
    }
    private void visibility(){
        check(searchHidden(),"new view has no search input or hidden focus target");
        int top=list().getY(),height=list().getHeight();String before=ConfigStore.encode(Neon.config);
        key(GLFW.GLFW_KEY_F,GLFW.GLFW_MOD_CONTROL);
        check(search().isFocused()&&list().getY()>top,"Ctrl+F reveals search and makes room above results");
        key(GLFW.GLFW_KEY_ESCAPE,0);
        check(searchHidden()&&list().getY()==top&&list().getHeight()==height,"Escape hides empty search and reclaims its entire row");
        check(ConfigStore.encode(Neon.config).equals(before),"showing and hiding search never changes saved settings");
    }
    private AbstractScrollArea list(){return widgets(screen()).stream().filter(w->w.getClass().getSimpleName().equals("UiScroll")).map(AbstractScrollArea.class::cast).findFirst().orElseThrow();}
    private void key(int key,int mods){screen().keyPressed(new KeyEvent(key,0,mods));screen().keyReleased(new KeyEvent(key,0,mods));}
    private void query(String value){
        key(GLFW.GLFW_KEY_F,GLFW.GLFW_MOD_CONTROL);check(search().isFocused(),"Ctrl+F focuses the search field");
        key(GLFW.GLFW_KEY_A,GLFW.GLFW_MOD_CONTROL);key(GLFW.GLFW_KEY_BACKSPACE,0);
        String before=ConfigStore.encode(Neon.config);int cues=SoundAudit.cues.size();
        value.codePoints().forEach(cp->check(screen().charTyped(new CharacterEvent(cp)),"search accepts typed character"));
        check(search().getValue().equals(value)&&search().isFocused(),"typed filter and caret survive result changes");
        check(ConfigStore.encode(Neon.config).equals(before),"filtering never changes settings or HUD visibility");
        check(cues==SoundAudit.cues.size(),"continuous search typing is silent");
    }
    private void click(AbstractWidget w,int button){var s=screen();var e=new MouseButtonEvent(s.guiCoordinate(w.getX()+w.getWidth()/2d),s.guiCoordinate(w.getY()+10),new MouseButtonInfo(button,0));check(s.mouseClicked(e,false),"search result accepts native pointer path");s.mouseReleased(e);}
    private void escapeWithHudColor() {
        click(named("Hex color"), 0);
        key(GLFW.GLFW_KEY_ESCAPE, 0);
        check(search().getValue().equals("fps"), "the active hex edit receives Escape before search");
        key(GLFW.GLFW_KEY_ESCAPE, 0);
        check(search().getValue().isEmpty() && named("Color picker") != null, "clearing HUD search keeps its color editor");
        key(GLFW.GLFW_KEY_ESCAPE, 0);
        check(searchHidden() && named("Color picker") != null, "hiding HUD search keeps its color editor");
        key(GLFW.GLFW_KEY_ESCAPE, 0);
        check(widgets(editor).stream().noneMatch(w -> w.getMessage().getString().equals("Color picker")), "ordinary back navigation resumes");
    }
    private List<String> tiles(){return widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiModuleTile")).map(w->w.getMessage().getString()).toList();}
    private void fit(){var s=screen();var b=s instanceof SettingsScreen settings?settings.expandedBounds():((HudEditor)s).inspectorBounds();var field=search();check(b.left()>=0&&b.top()>=0&&b.right()<=s.canvasWidth()&&b.bottom()<=s.canvasHeight(),"searched shell fits the viewport");check(field.getX()>b.left()&&field.getRight()<b.right()&&field.getY()>b.top()&&field.getBottom()<b.bottom(),"search stays within its shared shell");check(list().getHeight()>=22&&list().getY()>field.getBottom(),"search leaves usable results below it");}
    private void shot(String name){Screenshot.grab(mc.gameDirectory,"neon-search-"+name+"-"+variant+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});}
    boolean run()throws Exception{
        if(++age<9)return false;age=0;
        switch(stage++){
            case 0->{
                Neon.applyConfig(new NeonConfig());mc.getWindow().setWindowed(variant==2?640:1280,variant==2?360:800);mc.options.guiScale().set(variant+1);mc.resizeGui();mc.setLastInputType(InputType.MOUSE);
                new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"),Neon.SAVES);Neon.SAVES.flush().join();mc.gui.setScreen(menu=Menus.fresh(null));
                if(variant>0){var b=menu.expandedBounds();var down=new MouseButtonEvent(menu.guiCoordinate(b.right()-1),menu.guiCoordinate(b.bottom()-1),new MouseButtonInfo(0,0));var end=new MouseButtonEvent(down.x()-menu.guiCoordinate(120),down.y()-menu.guiCoordinate(70),down.buttonInfo());menu.mouseClicked(down,false);menu.mouseDragged(end,end.x()-down.x(),end.y()-down.y());menu.mouseReleased(end);}
                visibility();
            }
            case 1->{
                shot("module-hidden");
                list().setScrollAmount(Math.min(100,list().maxScrollAmount()));gridPosition=list().scrollAmount();query("  bEtTeR chAt  ");
                check(tiles().equals(List.of("Better chat")),"module query ignores case and surrounding spaces");
                key(GLFW.GLFW_KEY_RIGHT_SHIFT,0);check(mc.gui.screen()==menu,"alternate menu binding does not escape focused search");
                key(GLFW.GLFW_KEY_ENTER,0);check(!search().isFocused(),"Enter moves focus into matching results");
                key(GLFW.GLFW_KEY_F10,GLFW.GLFW_MOD_SHIFT);
            }
            case 2->{
                fit();shot("module");
                query("chat");escapeFromResult(GLFW.GLFW_KEY_ENTER,gridPosition);
                check(named("Message bar")!=null,"Escape from a result keeps the Better chat drawer open");
                query("no such module");check(tiles().isEmpty(),"unmatched query has no interactive module targets");check(named("No matching modules")!=null,"empty results have a readable message");key(GLFW.GLFW_KEY_ESCAPE,0);check(search().getValue().isEmpty()&&tiles().size()==21,"Escape clears the filter before closing a drawer");check(Math.abs(list().scrollAmount()-gridPosition)<1,"clearing a module filter restores its unfiltered scroll");key(GLFW.GLFW_KEY_ESCAPE,0);check(searchHidden()&&named("Message bar")!=null,"second Escape hides search while preserving the module drawer");click(named("HUD editor"),0);editor=(HudEditor)mc.gui.screen();visibility();
            }
            case 3->{shot("hud-hidden");list().setScrollAmount(100);hudPosition=list().scrollAmount();query("fPs");escapeFromResult(GLFW.GLFW_KEY_DOWN,hudPosition);query("fPs");}
            case 4->{fit();check(widgets(editor).stream().filter(w->w.getClass().getSimpleName().equals("HudEntry")).map(w->w.getMessage().getString()).toList().equals(List.of("FPS")),"HUD search filters rows without showing unrelated widgets");key(GLFW.GLFW_KEY_ESCAPE,0);check(Math.abs(list().scrollAmount()-hudPosition)<1,"clearing HUD search restores its unfiltered scroll");key(GLFW.GLFW_KEY_ESCAPE,0);check(searchHidden()&&mc.gui.screen()==editor,"second Escape hides HUD search without closing the editor");query("fps");click(named("FPS"),1);}
            case 5->{fit();shot("hud");var row=named("Label color");if(row.getBottom()>list().getBottom())list().setScrollAmount(list().scrollAmount()+row.getBottom()-list().getBottom());click(row,0);}
            case 6->{fit();shot("hud-color");check(widgets(editor).stream().anyMatch(w->w.getClass().getSimpleName().equals("ColorPicker")),"searched FPS fold uses the existing attached color drawer");escapeWithHudColor();query("inventory");check(!Neon.config.hud(me.xchro.neon.hud.HudKind.INVENTORY).placement.visible,"disabled HUD entries remain searchable without enabling them");var b=FpsHud.bounds(mc.getWindow().getWidth(),mc.getWindow().getHeight());var e=new MouseButtonEvent((b.x()+b.width()/2d)/editor.pixelScale(),(b.y()+b.height()/2d)/editor.pixelScale(),new MouseButtonInfo(0,0));check(editor.mouseClicked(e,false),"enabled canvas widget remains clickable while filtered out of the column");editor.mouseReleased(e);check(search().getValue().isEmpty(),"canvas selection clears a filter hiding the selected settings");}
            case 7->{
                check(!search().isFocused(),"canvas selection does not leave search holding keyboard focus");
                var b=FpsHud.bounds(mc.getWindow().getWidth(),mc.getWindow().getHeight());key(GLFW.GLFW_KEY_LEFT,0);
                check(FpsHud.bounds(mc.getWindow().getWidth(),mc.getWindow().getHeight()).x()==b.x()-1,"canvas selection retains one-pixel keyboard nudging");
                key(GLFW.GLFW_KEY_Z,GLFW.GLFW_MOD_CONTROL);check(FpsHud.bounds(mc.getWindow().getWidth(),mc.getWindow().getHeight()).equals(b),"Undo restores a nudge after clearing a canvas filter");
                query("not a widget");check(named("No matching HUD elements")!=null,"empty HUD query has a readable result");key(GLFW.GLFW_KEY_ESCAPE,0);check(search().getValue().isEmpty(),"Escape clears HUD search without leaving the editor");query("chat");key(GLFW.GLFW_KEY_H,0);check(editor.controlsVisible(),"H stays in focused search instead of hiding the column");query("");editor.onClose();check(mc.gui.screen()==menu&&searchHidden(),"HUD search returns to the same menu with search still hidden");click(named("Settings"),0);visibility();
            }
            case 8->{shot("settings-hidden");query("colors");escapeFromResult(GLFW.GLFW_KEY_ENTER,0);query("colors");key(GLFW.GLFW_KEY_ENTER,0);key(GLFW.GLFW_KEY_ENTER,0);}
            case 9->{fit();shot("settings");check(tiles().equals(List.of("Colors")),"Settings uses the same compact filtering");check(widgets(menu).stream().anyMatch(w->w.getMessage().getString().equals("Outline")),"filtered Settings section opens its existing controls");click(named("Combat"),0);check(searchHidden(),"another category starts with search hidden");click(named("Settings"),0);check(!searchHidden()&&search().getValue().equals("colors"),"returning to a filtered category restores its visible query");mc.gui.setScreen(null);if(++variant<3){stage=0;}else{Neon.applyConfig(original);return true;}}
        }
        return false;
    }
}

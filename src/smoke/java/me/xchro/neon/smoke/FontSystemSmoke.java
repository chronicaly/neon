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

/** Installed-family selection through the real settings drawer, including persistence and recovery. */
final class FontSystemSmoke {
    private final Minecraft mc;private final NeonConfig original;private SettingsScreen menu;
    private int checks,stage,age;private String chosen,profile;
    private AbstractScrollArea fontList;
    FontSystemSmoke(Minecraft mc,int checks){this.mc=mc;this.checks=checks;original=ConfigStore.copy(Neon.config);}
    int checks(){return checks;}
    private void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;System.out.println("NEON_CHECK "+message);}
    private List<AbstractWidget> widgets(ContainerEventHandler parent){var result=new ArrayList<AbstractWidget>();for(var child:parent.children()){if(child instanceof AbstractWidget w)result.add(w);if(child instanceof ContainerEventHandler group)result.addAll(widgets(group));}return result;}
    private Button button(String name){return widgets(menu).stream().filter(w->w instanceof Button&&w.getMessage().getString().equals(name)).map(Button.class::cast).findFirst().orElseThrow();}
    private EditBox search(){return widgets(menu).stream().filter(w->w instanceof EditBox&&w.getMessage().getString().equals("Search fonts")).map(EditBox.class::cast).findFirst().orElseThrow();}
    private void click(Button button){var e=new MouseButtonEvent(menu.guiCoordinate(button.getX()+10),menu.guiCoordinate(button.getY()+10),new MouseButtonInfo(0,0));check(menu.mouseClicked(e,false),"font option responds to the real mouse path");menu.mouseReleased(e);}
    private void key(int key,int modifiers){menu.keyPressed(new KeyEvent(key,0,modifiers));}
    private String renderedFamily()throws Exception{Ui.width("Neon Ag 0123456789");var field=Class.forName("me.xchro.neon.ui.UiFont").getDeclaredField("bodyFont");field.setAccessible(true);return ((java.awt.Font)field.get(null)).getFamily(Locale.ROOT);}
    private void fit(){
        var picker=widgets(menu).stream().filter(w->w.getMessage().getString().equals("Installed fonts")).findFirst().orElseThrow();
        var bounds=menu.expandedBounds();check(picker.getX()>=bounds.left()&&picker.getRight()<=bounds.right()&&picker.getBottom()<=bounds.bottom(),"font picker fits the shared drawer");
        check(search().getX()>picker.getX()&&search().getRight()<picker.getRight(),"font search stays inset inside the drawer");
        var lists=widgets((ContainerEventHandler)picker).stream().filter(w->w instanceof AbstractScrollArea).map(AbstractScrollArea.class::cast).toList();
        check(lists.size()==1&&lists.getFirst().getHeight()>=26,"font results retain usable height at this menu size");
    }
    private void shot(String name){Screenshot.grab(mc.gameDirectory,"neon-fonts-"+name+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});}
    boolean run()throws Exception {
        if(++age<8)return false;age=0;
        switch(stage++){
            case 0->{
                Neon.applyConfig(new NeonConfig());mc.getWindow().setWindowed(1280,800);mc.options.guiScale().set(2);mc.resizeGui();
                new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"),Neon.SAVES);Neon.SAVES.flush().join();
                mc.gui.setScreen(Menus.fresh(null));menu=(SettingsScreen)mc.gui.screen();button("Settings").onPress(null);Menus.configure(menu,"Fonts");
                chosen=FontCatalog.available("Papyrus")?FontCatalog.canonical("Papyrus"):FontCatalog.canonical("Serif");
            }
            case 1->{
                fit();check(FontCatalog.families().size()>1,"font chooser enumerates installed PC fonts");
                int cues=SoundAudit.cues.size();search().setValue(chosen);
                check(SoundAudit.cues.size()==cues,"font search is silent");click(button(chosen));
                check(Neon.config.clientFont.equals(chosen),"font selection applies immediately");
                check(renderedFamily().equals(chosen),"menu renderer resolves the selected installed font");
            }
            case 2->{
                fit();shot("selected-default");check(search().getValue().equals(chosen),"font selection preserves the search query");
                int cues=SoundAudit.cues.size();button(chosen).onPress(null);check(SoundAudit.cues.size()==cues,"choosing the current font stays silent");
                key(GLFW.GLFW_KEY_Z,GLFW.GLFW_MOD_CONTROL);check(Neon.config.clientFont.equals("Inter"),"one Undo restores the previous font");
                button(chosen).onPress(null);Neon.save();Neon.SAVES.flush().join();
                check(new ConfigStore(mc.gameDirectory.toPath().resolve("config/neon.json")).load().clientFont.equals(chosen),"font family survives settings save and reload");
                profile="Font QA "+System.nanoTime();Neon.profiles.putAsync(profile,Neon.config).join();
                check(Neon.profiles.get(profile).clientFont.equals(chosen),"font family survives profile serialization");
            }
            case 3->{
                var b=menu.expandedBounds();var down=new MouseButtonEvent(menu.guiCoordinate(b.right()-1),menu.guiCoordinate(b.bottom()-1),new MouseButtonInfo(0,0));
                var end=new MouseButtonEvent(down.x()-menu.guiCoordinate(120),down.y()-menu.guiCoordinate(70),down.buttonInfo());
                menu.mouseClicked(down,false);menu.mouseDragged(end,end.x()-down.x(),end.y()-down.y());menu.mouseReleased(end);
                check(menu.menuBounds().width()==320&&menu.menuBounds().height()==210,"font drawer supports the minimum menu size with search focus: "+menu.menuBounds());
            }
            case 4->{
                fit();shot("selected-minimum");search().setValue("no installed font matches this query");
                check(widgets(menu).stream().anyMatch(w->w.getMessage().getString().equals("No matching fonts")),"empty font searches show a clear result");
                search().setValue("");
                var picker=widgets(menu).stream().filter(w->w.getMessage().getString().equals("Installed fonts")).findFirst().orElseThrow();
                var list=widgets((ContainerEventHandler)picker).stream().filter(w->w instanceof AbstractScrollArea).map(AbstractScrollArea.class::cast).findFirst().orElseThrow();
                check(list.maxScrollAmount()>0,"installed font list remains scrollable without a visible scrollbar");
                fontList=list;check(menu.mouseScrolled(menu.guiCoordinate(list.getX()+20),menu.guiCoordinate(list.getY()+20),0,-3),"wheel input reaches the nested font list");
            }
            case 5->{
                check(fontList.scrollAmount()>0,"font list scrolls while the search remains fixed");fontList.setScrollAmount(0);
                button("Inter").onPress(null);check(Neon.config.clientFont.equals("Inter"),"bundled Inter remains selectable");
                var missing=ConfigStore.copy(Neon.config);missing.clientFont="Neon unavailable font verification";Neon.applyConfig(missing);
                check(renderedFamily().toLowerCase(Locale.ROOT).contains("inter"),"missing profile fonts fall back to bundled Inter");
                check(Neon.config.clientFont.equals(missing.clientFont),"missing font fallback preserves the saved family name");
            }
            case 6->{mc.getWindow().setWindowed(640,360);mc.options.guiScale().set(3);mc.resizeGui();}
            case 7->{fit();shot("small-display");}
            default->{Neon.profiles.delete(profile);Neon.applyConfig(original);mc.gui.setScreen(null);return true;}
        }
        return false;
    }
}

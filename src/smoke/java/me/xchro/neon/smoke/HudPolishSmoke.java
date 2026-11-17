package me.xchro.neon.smoke;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.chat.*;
import me.xchro.neon.config.*;
import me.xchro.neon.hud.*;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.*;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.*;
import org.lwjgl.glfw.GLFW;

/** Actual pointer gestures, fold frames, native chat click collection and installed font layout. */
final class HudPolishSmoke {
    private final Minecraft mc;
    private final NeonConfig original;
    private final ChatComponent.State chatBefore;
    private HudEditor editor;
    private int checks,stage,age,variant,foldStart;
    private double outlinePhase;
    private long foldOpened;
    private HudBounds before;
    private Component linked;
    private ChatDockSmoke dock;
    HudPolishSmoke(Minecraft mc,int checks){this.mc=mc;this.checks=checks;original=ConfigStore.copy(Neon.config);chatBefore=mc.gui.hud.getChat().storeState();}
    int checks(){return checks;}
    private void check(boolean ok,String message){if(!ok)throw new AssertionError("HUD polish "+stage+": "+message);checks++;System.out.println("NEON_CHECK "+message);}
    private Object field(Object o,String name)throws Exception{for(Class<?> type=o.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private MouseButtonEvent pointer(double x,double y,int modifiers){return new MouseButtonEvent(x/editor.pixelScale(),y/editor.pixelScale(),new MouseButtonInfo(0,modifiers));}
    private List<AbstractWidget> widgets(ContainerEventHandler root){var list=new ArrayList<AbstractWidget>();for(var child:root.children()){if(child instanceof AbstractWidget w)list.add(w);if(child instanceof ContainerEventHandler nested)list.addAll(widgets(nested));}return list;}
    private void place(HudKind kind,int x,int y){var card=HudWidgets.get(kind);card.bounds(1280,800,true).move(Neon.config.hud(kind).placement,x,y,1280,800);}
    private void undo(){editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z,0,GLFW.GLFW_MOD_CONTROL));}
    private AbstractWidget fold(String key)throws Exception{return (AbstractWidget)((Map<?,?>)field(editor,"folds")).get(key);}
    private void click(HudBounds b){var e=pointer(b.x()+b.width()/2d,b.y()+b.height()/2d,0);check(editor.mouseClicked(e,false),"widget accepts canvas click");check(editor.controlsVisible(),"simple pointer down keeps the inspector visible");editor.mouseDragged(pointer(b.x()+b.width()/2d+1,b.y()+b.height()/2d,0),0,0);editor.mouseReleased(e);}
    private double phase()throws Exception{
        var state=new GuiRenderState();editor.extractRenderState(new GuiGraphicsExtractor(mc,state,0,0),0,0,0);
        var animated=new ArrayList<Object>();state.forEachElement(e->{if(e.getClass().getSimpleName().equals("Stroke"))try{if((boolean)field(e,"animated"))animated.add(e);}catch(Exception error){throw new AssertionError(error);}},GuiRenderState.TraverseRange.ALL);
        check(animated.size()==1,"HUD column uses one continuous animated outline");return (double)field(animated.getFirst(),"phase");
    }
    private void shot(String name){Screenshot.grab(mc.gameDirectory,"neon-hud-polish-"+name+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});}
    boolean run()throws Exception {
        age++;
        switch(stage){
            case 0->{
                mc.getWindow().setWindowed(1280,800);mc.options.guiScale().set(2);mc.options.framerateLimit().set(120);mc.resizeGui();mc.setLastInputType(InputType.MOUSE);
                Neon.applyConfig(new NeonConfig());var c=Neon.config;c.armor.visible=c.totems.visible=false;c.fpsBackground=c.fpsBorder=true;
                c.hud(HudKind.MEMORY).placement.visible=true;place(HudKind.MEMORY,500,500);FpsHud.bounds(1280,800).move(c.fps,330,503,1280,800);
                new HudPanelPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-hud-window.json"),Neon.SAVES);Neon.SAVES.flush().join();mc.gui.setScreen(editor=new HudEditor(null));stage++;age=0;
            }
            case 1->{
                if(age<4)return false;before=FpsHud.bounds(1280,800);double gx=before.width()/2d,gy=before.height()/2d;
                var start=pointer(before.x()+gx,before.y()+gy,0);check(editor.mouseClicked(start,false),"FPS begins a real pointer gesture");
                var neighbor=HudWidgets.get(HudKind.MEMORY).bounds(1280,800,true);int edge=neighbor.x()-before.width()-6;var caught=pointer(edge-3+gx,neighbor.y()+3+gy,0);editor.mouseDragged(caught,0,0);
                var b=FpsHud.bounds(1280,800);check(b.x()==edge&&b.y()==neighbor.y(),"cluster catches and aligns: "+before+" -> "+b+" beside "+neighbor+" expected x="+edge);
                var push=pointer(edge+8+gx,503+gy,0);editor.mouseDragged(push,0,0);check(FpsHud.bounds(1280,800).x()==edge,"small pushes retain the cluster snap");
                var force=pointer(edge+36+gx,503+gy,0);editor.mouseDragged(force,0,0);check(FpsHud.bounds(1280,800).x()>edge+6,"deliberate additional movement permits overlap");editor.mouseReleased(force);
                check(!((Set<?>)field(editor,"expanded")).contains("FPS"),"dragging does not open widget settings");undo();check(FpsHud.bounds(1280,800).equals(before),"Undo restores the entire snapped-and-forced gesture");
                start=pointer(before.x()+gx,before.y()+gy,GLFW.GLFW_MOD_ALT);editor.mouseClicked(start,false);var bypass=pointer(edge+4+gx,503+gy,GLFW.GLFW_MOD_ALT);editor.mouseDragged(bypass,0,0);editor.mouseReleased(bypass);check(FpsHud.bounds(1280,800).x()==edge+4,"Alt bypasses the edge magnet immediately");undo();
                click(before);check(((Set<?>)field(editor,"expanded")).contains("FPS"),"canvas click opens the selected widget fold");check(FpsHud.bounds(1280,800).equals(before),"click tolerance does not change saved placement");foldStart=fold("FPS").getHeight();foldOpened=System.nanoTime();check(foldStart<(int)field(fold("FPS"),"fullHeight"),"click starts a smooth fold rather than jumping to full height");stage++;age=0;
            }
            case 2->{
                // Tick catch-up after font/page sweeps may run before another render updates the fold.
                editor.extractRenderState(new GuiGraphicsExtractor(mc,new GuiRenderState(),0,0),0,0,0);
                if(fold("FPS").getHeight()<=foldStart&&System.nanoTime()-foldOpened<1_000_000_000L)return false;
                check(fold("FPS").getHeight()>foldStart,"widget settings animate through a growing fold");stage++;age=0;
            }
            case 3->{
                if(age<5)return false;editor.extractRenderState(new GuiGraphicsExtractor(mc,new GuiRenderState(),0,0),0,0,0);
                if(fold("FPS").getHeight()<(int)field(fold("FPS"),"fullHeight")&&System.nanoTime()-foldOpened<1_000_000_000L)return false;
                check(fold("FPS").getHeight()==(int)field(fold("FPS"),"fullHeight"),"widget fold settles completely");outlinePhase=phase();shot("editor");
                var memory=HudWidgets.get(HudKind.MEMORY).bounds(1280,800,true);var fps=FpsHud.bounds(1280,800);fps.move(Neon.config.fps,memory.x()-fps.width()-6,memory.y(),1280,800);stage=20;age=0;
            }
            case 20->{
                if(age<4)return false;shot("cluster");
                var row=widgets(editor).stream().filter(w->w.getMessage().getString().equals("Label color")).findFirst().orElseThrow();
                var e=new MouseButtonEvent(editor.guiCoordinate(row.getX()+12),editor.guiCoordinate(row.getY()+8),new MouseButtonInfo(0,0));
                check(editor.mouseClicked(e,false),"canvas-selected settings open their attached color drawer");editor.mouseReleased(e);stage++;age=0;
            }
            case 21->{
                if(age<9)return false;outlinePhase=phase();check(editor.inspectorBounds().width()>300,"attached color drawer expands the HUD shell");shot("outline-drawer");
                editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE,0,0));stage++;age=0;
            }
            case 22->{if(age<9)return false;phase();check(editor.inspectorBounds().width()<300,"dismissing colors restores the compact animated column");stage=4;age=0;}
            case 4->{if(age<3)return false;check(Math.abs(phase()-outlinePhase)>.1,"HUD editor highlight travels over time");
                Neon.config.hud(HudKind.COORDINATES).placement.visible=true;place(HudKind.COORDINATES,700,450);HudWidgets.invalidate();click(HudWidgets.get(HudKind.COORDINATES).bounds(1280,800,true));
                check(widgets(editor).stream().noneMatch(w->w.getMessage().getString().startsWith("Dimension")),"Coordinates no longer offers a dimension-text toggle");
                var card=HudWidgets.get(HudKind.COORDINATES);card.invalidate();card.hasContent(false);check(((String[])field(card,"lines")).length==1,"Coordinates always displays a single coordinate row");stage++;age=0;
            }
            case 5->{if(age<5)return false;shot("coordinates");stage++;age=0;}
            case 6->{
                mc.gui.setScreen(null);mc.options.guiScale().set(variant+1);mc.options.chatScale().set(variant==1?.85:1d);mc.resizeGui();
                Neon.config.hud(HudKind.CHAT).placement.visible=true;Neon.config.hud(HudKind.CHAT).placement.scale=.8;Neon.config.hud(HudKind.CHAT).placement.x=.1;Neon.config.hud(HudKind.CHAT).placement.y=.5;
                Neon.config.clientFont=variant==2&&FontCatalog.available("Papyrus")?"Papyrus":"Inter";Neon.config.betterChat.clientFont=true;
                linked=Component.literal("Click: ").append(Component.literal("Neon WWW ill 0123").withStyle(s->s.withBold(true).withItalic(true).withClickEvent(new ClickEvent.SuggestCommand("/neon-proof"))));
                var text=BetterChat.display(linked,GuiMessageSource.PLAYER).getVisualOrderText();
                check(Math.abs(mc.font.width(text)-ChatText.width(text))<=1,"native wrapping shares the rendered proportional font advances");
                var end=new ActiveTextCollector.ClickableStyleFinder(mc.font,(int)(ChatText.width(text)-1),3);end.accept(0,0,text);check(end.result()!=null&&end.result().getClickEvent()!=null,"last visible styled character remains clickable");
                var chat=mc.gui.hud.getChat();chat.clearMessages(false);chat.addPlayerMessage(linked,null,null);mc.gui.setScreen(new ChatScreen("/",false));stage++;age=0;
            }
            case 7->{
                if(age<6)return false;var chat=mc.gui.hud.getChat();var b=ChatHud.displayed();int scale=NeonHud.gameScale();boolean hit=false;
                for(int y=(b.y()+b.height())/scale-70;y<(b.y()+b.height())/scale&&!hit;y+=2)for(int x=b.x()/scale;x<(b.x()+b.width())/scale;x+=2){
                    var finder=new ActiveTextCollector.ClickableStyleFinder(mc.font,x,y);chat.captureClickableText(finder,mc.getWindow().getGuiScaledHeight(),mc.gui.hud.getGuiTicks(),ChatComponent.DisplayMode.FOREGROUND);
                    if(finder.result()!=null&&finder.result().getClickEvent()!=null){hit=true;break;}
                }
                check(hit,"native chat pointer collection follows the moved/scaled Chat HUD at GUI scale "+scale);shot("chat-scale"+scale);
                if(++variant<3)stage=6;else stage++;age=0;
            }
            case 8->{Neon.config.betterChat.clientFont=false;BetterChat.tick(mc);check(!ChatText.uses(BetterChat.display(linked,GuiMessageSource.PLAYER).getVisualOrderText()),"Client font off restores native Minecraft chat rendering");stage++;age=0;}
            case 9->{
                if(age<5)return false;shot("chat-native");var chat=mc.gui.hud.getChat();chat.clearMessages(false);mc.gui.setScreen(editor=new HudEditor(null));
                var card=HudWidgets.get(HudKind.CHAT);var graphics=new GuiGraphicsExtractor(mc,new GuiRenderState(),0,0);card.draw(graphics,1280,800,true);
                check(!(boolean)field(card,"messages"),"empty Chat HUD preview uses its placeholder without inserting history");chat.addPlayerMessage(Component.literal("Arrived while editing"),null,null);
                card.draw(graphics,1280,800,true);check((boolean)field(card,"messages"),"Chat HUD preview picks up messages arriving while the editor stays open");
                Neon.applyConfig(original);chat.restoreState(chatBefore);mc.gui.setScreen(null);dock=new ChatDockSmoke(mc,checks);stage=30;
            }
            case 30->{if(dock.run()){checks=dock.checks();return true;}}
        }
        return false;
    }
}

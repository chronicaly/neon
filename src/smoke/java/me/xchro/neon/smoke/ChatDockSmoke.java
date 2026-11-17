package me.xchro.neon.smoke;

import java.util.function.Consumer;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.hud.*;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.*;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

/** Packaged checks of the native icon renderer, shared chat pose and moving bottom-left stack. */
final class ChatDockSmoke {
    private final Minecraft mc;
    private final NeonConfig original;
    private final ChatComponent.State history;
    private int checks,stage,variant;
    private long since;
    private HudBounds baseChat,baseCoordinates;
    private String saved;
    ChatDockSmoke(Minecraft mc,int checks){this.mc=mc;this.checks=checks;original=ConfigStore.copy(Neon.config);history=mc.gui.hud.getChat().storeState();}
    int checks(){return checks;}
    private void check(boolean ok,String message){if(!ok)throw new AssertionError("Chat dock "+stage+": "+message);checks++;System.out.println("NEON_CHECK "+message);}
    private static final class Capture implements ChatComponent.ChatGraphicsAccess {
        final Matrix3x2f pose=new Matrix3x2f();
        Vector2f first;
        public void updatePose(Consumer<Matrix3x2f> update){update.accept(pose);}
        public void fill(int a,int b,int c,int d,int color){}
        public boolean handleMessage(int y,float opacity,FormattedCharSequence text){if(first==null)first=pose.transformPosition(0,y,new Vector2f());return true;}
        public void handleTag(int a,int b,int c,int d,float opacity,GuiMessageTag tag){}
        public void handleTagIcon(int a,int b,boolean selected,GuiMessageTag tag,GuiMessageTag.Icon icon){}
    }
    private Capture capture()throws Exception {
        var result=new Capture();var method=ChatComponent.class.getDeclaredMethod("extractRenderState",ChatComponent.ChatGraphicsAccess.class,int.class,int.class,ChatComponent.DisplayMode.class);
        method.setAccessible(true);method.invoke(mc.gui.hud.getChat(),result,mc.getWindow().getGuiScaledHeight(),mc.gui.hud.getGuiTicks(),ChatComponent.DisplayMode.FOREGROUND);return result;
    }
    private HudBounds coordinates()throws Exception {
        var method=HudCard.class.getDeclaredMethod("renderBounds",int.class,int.class,boolean.class);method.setAccessible(true);
        return (HudBounds)method.invoke(HudWidgets.get(HudKind.COORDINATES),mc.getWindow().getWidth(),mc.getWindow().getHeight(),false);
    }
    private int icons()throws Exception {
        var state=new GuiRenderState();var g=new GuiGraphicsExtractor(mc,state,0,0);
        var method=Hud.class.getDeclaredMethod("extractEffects",GuiGraphicsExtractor.class,DeltaTracker.class);method.setAccessible(true);method.invoke(mc.gui.hud,g,DeltaTracker.ZERO);
        int[] count={0};state.forEachElement(e->count[0]++,GuiRenderState.TraverseRange.ALL);return count[0];
    }
    private void iconChecks()throws Exception {
        var effects=mc.player.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        try {
            mc.player.getActiveEffectsMap().clear();mc.player.addEffect(new MobEffectInstance(MobEffects.SPEED,1800,1));
            for(int mask=0;mask<16;mask++){
                Neon.config.hud(HudKind.POTIONS).placement.visible=(mask&1)!=0;Neon.config.potionHideVanillaIcons=(mask&2)!=0;
                ClientModule.CLEAN_VIEW.enabled(Neon.config,(mask&4)!=0);Neon.config.hideStatusEffects=(mask&8)!=0;
                boolean hidden=(mask&3)==3||(mask&12)==12;
                check((icons()==0)==hidden,"native effect icons obey independent HUD/Clean view switches, combination "+mask);
                check(mc.player.hasEffect(MobEffects.SPEED)&&mc.player.getEffect(MobEffects.SPEED).getAmplifier()==1,"icon visibility preserves the active effect");
            }
        } finally {mc.player.getActiveEffectsMap().clear();effects.forEach(mc.player::addEffect);}
        Neon.config.hud(HudKind.POTIONS).placement.visible=false;Neon.config.potionHideVanillaIcons=Neon.config.hideStatusEffects=false;ClientModule.CLEAN_VIEW.enabled(Neon.config,false);
    }
    private void linkCheck(Capture capture){
        check(capture.first!=null,"native chat emits its first linked line");boolean found=false;
        for(int y=(int)capture.first.y-4;y< capture.first.y+14&&!found;y++)for(int x=(int)capture.first.x;x<capture.first.x+40;x++){
            var finder=new ActiveTextCollector.ClickableStyleFinder(mc.font,x,y);
            mc.gui.hud.getChat().captureClickableText(finder,mc.getWindow().getGuiScaledHeight(),mc.gui.hud.getGuiTicks(),ChatComponent.DisplayMode.FOREGROUND);
            if(finder.result()!=null&&finder.result().getClickEvent()!=null){found=true;break;}
        }
        check(found,"native link hitbox follows the gap-free chat pose at GUI scale "+NeonHud.gameScale());
    }
    private int frame()throws Exception {
        NeonHud.beginFrame();try {
            var chat=ChatHud.displayed();var coords=coordinates();
            int delta=baseCoordinates.y()-coords.y();
            check(baseChat.y()+baseChat.height()-chat.y()-chat.height()==delta,"chat and coordinates use exactly the same frame displacement");
            check(chat.y()+chat.height()+6==coords.y(),"bottom-left stack retains six pixels without overlapping");
            check(chat.y()>=0&&coords.y()>=0,"stack stays inside the display during motion");
            check(ConfigStore.encode(Neon.config).equals(saved),"render-only docking preserves saved positions and preferences");
            return delta;
        } finally {NeonHud.endFrame();}
    }
    private void shot(String name){Screenshot.grab(mc.gameDirectory,"neon-chat-dock-"+name+"-"+variant+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});}
    boolean run()throws Exception {
        switch(stage){
            case 0->{
                mc.gui.setScreen(null);mc.getWindow().setWindowed(variant==2?640:1280,variant==2?360:800);mc.options.guiScale().set(variant+1);mc.resizeGui();
                Neon.applyConfig(new NeonConfig());Neon.config.armor.visible=Neon.config.totems.visible=Neon.config.fps.visible=false;
                Neon.config.betterChat.enabled=false;mc.options.chatScale().set(variant==1?.85:1d);
                if(variant==0)iconChecks();
                var chat=mc.gui.hud.getChat();chat.clearMessages(false);
                for(int i=0;i<40;i++)chat.addPlayerMessage(Component.literal("Neon linked message "+i).withStyle(s->s.withClickEvent(new ClickEvent.SuggestCommand("/neon-proof"))),null,null);
                mc.gui.setScreen(new ChatScreen("",false));var normal=capture();Neon.config.betterChat.messageBar=false;var hidden=capture();
                check(Math.abs(normal.first.x-hidden.first.x-4*mc.options.chatScale().get())<.01,"hiding the native line removes its four-unit text gutter");linkCheck(hidden);mc.gui.setScreen(null);
                for(var kind:new HudKind[]{HudKind.CHAT,HudKind.COORDINATES}){var p=Neon.config.hud(kind).placement;p.visible=true;p.x=0;p.y=1;}
                check(HudChatDock.active(),"both enabled bottom-left widgets form a special stack");
                baseChat=ChatHud.reserved();baseCoordinates=HudWidgets.get(HudKind.COORDINATES).bounds(mc.getWindow().getWidth(),mc.getWindow().getHeight(),false);saved=ConfigStore.encode(Neon.config);
                check(frame()==0,"closed chat starts at its saved coordinate baseline");stage++;since=System.nanoTime();
            }
            case 1->{if(System.nanoTime()-since<300_000_000L)return false;shot("closed");mc.gui.setScreen(new ChatScreen("",false));check(frame()==0,"opening begins without a position jump");since=System.nanoTime();stage++;}
            case 2->{
                int moved=frame();if(System.nanoTime()-since<300_000_000L)return false;
                var input=(EditBox)mc.gui.screen().children().stream().filter(EditBox.class::isInstance).findFirst().orElseThrow();var coords=coordinates();
                check(moved>0&&coords.y()+coords.height()+4<=(input.getY()-2)*NeonHud.gameScale(),"settled stack clears the real chat input");
                NeonHud.beginFrame();try{linkCheck(capture());}finally{NeonHud.endFrame();}
                check(mc.gui.hud.getChat().getLinesPerPage()>0,"clipped chat keeps usable native scroll lines");shot("open");
                mc.gui.setScreen(null);check(frame()==moved,"closing starts at the current position");since=System.nanoTime();stage++;
            }
            case 3->{
                int moved=frame();if(System.nanoTime()-since<60_000_000L)return false;
                mc.gui.setScreen(new ChatScreen("",false));check(Math.abs(frame()-moved)<=2,"rapid reopening preserves the in-flight position");since=System.nanoTime();stage++;
            }
            case 4->{frame();if(System.nanoTime()-since<300_000_000L)return false;mc.gui.setScreen(null);frame();since=System.nanoTime();stage++;}
            case 5->{
                int moved=frame();if(System.nanoTime()-since<300_000_000L)return false;check(moved==0,"closing restores both original positions");
                Neon.config.hud(HudKind.COORDINATES).placement.x=.5;check(!HudChatDock.active(),"moving coordinates away releases the special stack");
                Neon.config.hud(HudKind.COORDINATES).placement.x=0;Neon.config.hud(HudKind.COORDINATES).placement.visible=false;check(!HudChatDock.active(),"disabled coordinates never reserve chat space");
                if(++variant<3){stage=0;return false;}
                Neon.applyConfig(original);mc.gui.hud.getChat().restoreState(history);mc.gui.setScreen(null);return true;
            }
        }
        return false;
    }
}

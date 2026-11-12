package me.xchro.neon.smoke;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.chat.BetterChat;
import me.xchro.neon.config.*;
import me.xchro.neon.core.UiSoundPolicy.Cue;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.glfw.GLFW;

/** Production chat pipeline and real controls; both audio engines remain intercepted. */
final class BetterChatSmoke {
    private final Minecraft mc;
    private final NeonConfig original = ConfigStore.copy(Neon.config);
    private final String binding = Neon.betterChat.saveString();
    private final ChatComponent chat;
    private final String name;
    private SettingsScreen menu;
    private int stage, age, checks;
    BetterChatSmoke(Minecraft mc, int checks) {
        this.mc=mc;this.checks=checks;chat=mc.gui.hud.getChat();name=mc.player.getGameProfile().name();
        Neon.config.betterChat=new NeonConfig.BetterChat();Neon.config.motionBlurEnabled=false;
        Neon.betterChat.setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN);KeyMapping.resetMapping();
        chat.clearMessages(false);
    }
    int checks(){return checks;}
    private void check(boolean ok,String message){if(!ok)throw new AssertionError("Better chat "+stage+": "+message);checks++;System.out.println("NEON_CHECK "+message);}
    private void expect(Runnable action,Cue... expected){int start=SoundAudit.cues.size();action.run();var actual=SoundAudit.cues.subList(start,SoundAudit.cues.size()).stream().map(r->r.cue()).toList();check(actual.equals(List.of(expected)),"chat cues "+actual+" match "+List.of(expected));}
    private void player(String text){chat.addPlayerMessage(Component.literal(text),null,null);}
    private Object field(Object object,String name)throws Exception{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    @SuppressWarnings("unchecked") private List<GuiMessage> history()throws Exception{return (List<GuiMessage>)field(chat,"allMessages");}
    @SuppressWarnings("unchecked") private String visible()throws Exception{var text=new StringBuilder();for(var line:(List<GuiMessage.Line>)field(chat,"trimmedMessages"))line.content().accept((index,style,codepoint)->{text.appendCodePoint(codepoint);return true;});return text.toString();}
    private List<AbstractWidget> widgets(ContainerEventHandler root){var result=new ArrayList<AbstractWidget>();for(var child:root.children()){if(child instanceof AbstractWidget w)result.add(w);if(child instanceof ContainerEventHandler nested)result.addAll(widgets(nested));}return result;}
    private AbstractScrollArea drawer(){return widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiScroll")).skip(1).map(w->(AbstractScrollArea)w).findFirst().orElseThrow();}
    private UiButton toggle(String label){return widgets(menu).stream().filter(w->w instanceof UiButton b&&b.hint().equals(label)).map(w->(UiButton)w).findFirst().orElseThrow();}
    private MouseButtonEvent pointer(double x,double y,int button){return new MouseButtonEvent(menu.guiCoordinate(x),menu.guiCoordinate(y),new MouseButtonInfo(button,0));}
    private void click(AbstractWidget w){var scroll=drawer();if(w.getBottom()>scroll.getBottom())scroll.setScrollAmount(scroll.scrollAmount()+w.getBottom()-scroll.getBottom());if(w.getY()<scroll.getY())scroll.setScrollAmount(scroll.scrollAmount()+w.getY()-scroll.getY());var event=pointer(w.getX()+w.getWidth()/2.,w.getY()+w.getHeight()/2.,0);check(menu.mouseClicked(event,false),"chat control accepts pointer");menu.mouseReleased(event);}
    private void setup(boolean minimum,int scale){
        new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"),Neon.SAVES);Neon.SAVES.flush().join();
        mc.getWindow().setWindowed(1280,800);mc.options.guiScale().set(scale);mc.resizeGui();mc.setLastInputType(InputType.MOUSE);
        mc.gui.setScreen(menu=Menus.fresh(null));
        if(minimum){var b=menu.menuBounds();var from=pointer(b.right()-1,b.bottom()-1,0);menu.mouseClicked(from,false);var to=pointer(b.right()-121,b.bottom()-71,0);menu.mouseDragged(to,0,0);menu.mouseReleased(to);}
        Menus.configure(menu,"Better chat");
    }
    private void geometry(){
        var bounds=menu.expandedBounds();check(bounds.left()>=0&&bounds.right()<=menu.canvasWidth(),"chat settings shell fits display");
        var detail=drawer();for(var w:widgets((ContainerEventHandler)detail))check(w.getX()>=detail.getX()&&w.getRight()<=detail.getRight(),"chat setting fits drawer: "+w.getMessage().getString());
        check(toggle("Message ping").active&&toggle("Mention ping").active,"both ping controls are available");
    }
    private void shot(String name){Screenshot.grab(mc.gameDirectory,"neon-chat-"+name+".png",mc.gameRenderer.mainRenderTarget(),1,c->{});}
    boolean run()throws Exception{
        if(++age<15)return false;age=0;
        switch(stage++){
            case 0->{
                expect(()->player("<Alex> hello"),Cue.CHAT_MESSAGE);
                expect(()->player("<Alex> hello"),Cue.CHAT_MESSAGE);
                expect(()->player("<Alex> hello"),Cue.CHAT_MESSAGE);
                check(history().size()==3,"duplicate originals remain in signed chat history");
                check(visible().contains("[x3]"),"Unity consecutive duplicate counter appears");
                expect(chat::rescaleChat);expect(()->chat.scrollChat(1));
                expect(()->player("<Alex> @"+name.toUpperCase(Locale.ROOT)+" hello"),Cue.CHAT_MENTION);
                expect(()->player("<Alex> "+name+"_2"),Cue.CHAT_MESSAGE);
                expect(()->player("<"+name+"> hello"),Cue.CHAT_MESSAGE);
                expect(()->chat.addPlayerMessage(Component.translatable("chat.type.text",Component.literal(name),Component.literal("hello")),null,null),Cue.CHAT_MESSAGE);
                expect(()->new ClientboundSystemChatPacket(Component.literal("[Server] @"+name),false).handle(mc.getConnection()),Cue.CHAT_MENTION);
                expect(()->new ClientboundSystemChatPacket(Component.literal("@"+name),true).handle(mc.getConnection()));
                expect(()->chat.addClientSystemMessage(Component.literal("Local settings: "+name)));
                expect(()->chat.setVisibleMessageFilter(m->false));expect(()->player("<Alex> hidden "+name));expect(()->chat.setVisibleMessageFilter(m->true));
            }
            case 1->{
                var click=new ClickEvent.SuggestCommand("/reply Alex ");
                var styled=Component.literal("<Alex> ").withColor(0x667788)
                    .append(Component.literal(name.substring(0,2)).withStyle(s->s.withBold(true).withClickEvent(click)))
                    .append(Component.literal(name.substring(2)).withStyle(s->s.withClickEvent(click)));
                var display=BetterChat.display(styled,GuiMessageSource.PLAYER);int[] highlighted={0};boolean[] bold={false};
                display.visit((style,text)->{if(style.getColor()!=null&&style.getColor().getValue()==0xffcd5a){highlighted[0]+=text.length();bold[0]|=style.isBold();check(click.equals(style.getClickEvent()),"mention retains clickable style");}return Optional.empty();},Style.EMPTY);
                check(highlighted[0]==name.length()&&bold[0],"name highlight spans components and retains bold");
                var signature=new MessageSignature(new byte[256]);expect(()->chat.addPlayerMessage(styled,signature,null),Cue.CHAT_MENTION);
                check(history().getFirst().content()==styled&&history().getFirst().signature()==signature,"original content and signature stay untouched");
                Neon.config.betterChat.mentions=false;expect(()->player("<Alex> "+name),Cue.CHAT_MENTION);
                check(BetterChat.display(styled,GuiMessageSource.PLAYER)==styled,"highlight can be disabled independently of mentions");
                Neon.config.betterChat.messagePing=false;expect(()->player("<Alex> normal"));expect(()->player("<Alex> "+name),Cue.CHAT_MENTION);
                Neon.config.betterChat.mentionPing=false;expect(()->player("<Alex> "+name));
                Neon.config.betterChat.messagePing=true;expect(()->player("<Alex> "+name),Cue.CHAT_MESSAGE);
                Neon.config.betterChat.volume=0;expect(()->player("<Alex> silent"));
                double master=mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).get();mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0d);
                Neon.config.theme.soundVolume=0;Neon.config.betterChat.volume=.23;
                expect(()->player("<Alex> independent volume"),Cue.CHAT_MESSAGE);check(Math.abs(SoundAudit.cues.getLast().volume()-.23)<1e-8,"ping volume is independent of Minecraft and UI volume");
                mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(master);Neon.config.theme.soundVolume=.4;
                Neon.config.betterChat.enabled=false;expect(()->player("<Alex> disabled"));expect(()->BetterChat.tick(mc));check(!visible().contains("[x3]"),"disabling restores ungrouped history silently");
                Neon.config.betterChat=new NeonConfig.BetterChat();expect(()->BetterChat.tick(mc));
                check(visible().contains("[x3]"),"reenabling rebuilds duplicate display without pinging");
                var audio=UiSounds.class.getDeclaredField("CLIPS");audio.setAccessible(true);check(((Map<?,?>)audio.get(null)).isEmpty(),"silent interception never opens audio clips");
                setup(false,1);
            }
            case 2->{geometry();shot("scale1-default");expect(()->click(toggle("Message ping")),Cue.DISABLE);check(!Neon.config.betterChat.messagePing,"native toggle changes message pings");menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z,0,GLFW.GLFW_MOD_CONTROL));check(Neon.config.betterChat.messagePing,"Undo restores chat setting");setup(true,1);}
            case 3->{geometry();shot("scale1-minimum");drawer().setScrollAmount(drawer().maxScrollAmount());}
            case 4->{shot("scale1-minimum-bottom");var color=widgets(menu).stream().filter(w->w instanceof Button&&w.getMessage().getString().equals("Mention color")).findFirst().orElseThrow();click(color);}
            case 5->{var popup=widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("ColorPicker")).findFirst().orElseThrow();check(popup.getX()>=0&&popup.getRight()<=menu.canvasWidth()&&popup.getBottom()<=menu.canvasHeight(),"mention popup fits minimum menu");shot("mention-color");menu.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE,0,0));setup(false,2);}
            case 6->{geometry();shot("scale2-default");setup(true,2);}
            case 7->{geometry();shot("scale2-minimum");setup(false,3);}
            case 8->{geometry();shot("scale3-default");setup(true,3);}
            case 9->{geometry();shot("scale3-minimum");mc.gui.setScreen(null);mc.gui.hud.setOverlayMessage(Component.empty(),false);chat.resetChatScroll();chat.clearMessages(false);player("<Alex> Testing Better chat");player("<Alex> Testing Better chat");player("<Alex> Hey @"+name+", ready?");}
            case 10->{shot("gameplay");Neon.applyConfig(original);Neon.betterChat.setKey(com.mojang.blaze3d.platform.InputConstants.getKey(binding));KeyMapping.resetMapping();return true;}
        }
        return false;
    }
}

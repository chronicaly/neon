package me.xchro.neon.hud;

import java.util.List;
import me.xchro.neon.Neon;
import me.xchro.neon.chat.ChatText;
import me.xchro.neon.config.ThemeColor;
import me.xchro.neon.core.UiMotion;
import me.xchro.neon.ui.HudEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;

/** Unity ChatHud: native lines, filtering, fade and click areas share one placement transform. */
public final class ChatHud extends HudCard {
    private static final HudChrome CHROME=new HudChrome();
    private static final UiMotion lift=new UiMotion(0);
    private static Object world;
    private static boolean messages;
    ChatHud(){super(HudKind.CHAT);}
    private static Minecraft mc(){return Minecraft.getInstance();}
    public static boolean enabled(){return Neon.config.hud(HudKind.CHAT).placement.visible;}
    private static float nativeScale(){return Math.max(.01f,mc().options.chatScale().get().floatValue());}
    private static int lineHeight(){return (int)(9*(mc().options.chatLineSpacing().get()+1));}
    private static int naturalWidth(){return (int)Math.ceil((Math.ceil(ChatComponent.getWidth(mc().options.chatWidth().get())/nativeScale())+(Neon.config.betterChat.messageBar?12:10))*nativeScale());}
    private static int bottomPadding(){return !HudChatDock.active()||mc().gui.chatListener().queueSize()>0?9:0;}
    private static int naturalHeight(){return (int)Math.ceil((ChatComponent.getHeight(mc().options.chatHeightFocused().get())/lineHeight()*lineHeight()+bottomPadding())*nativeScale());}
    private static float scale(){return (float)(HudBounds.scale(Neon.config.hud(HudKind.CHAT).placement,naturalWidth(),naturalHeight(),mc().getWindow().getWidth(),mc().getWindow().getHeight(),NeonHud.gameScale()*Neon.config.hudScale)/NeonHud.gameScale());}
    @Override protected void update(Minecraft mc,boolean preview){lines=preview?new String[]{"Chat messages appear here"}:new String[0];}
    @Override public boolean hasContent(boolean preview){return preview;}
    @Override public HudBounds bounds(int width,int height,boolean preview){
        var b=HudBounds.of(Neon.config.hud(HudKind.CHAT).placement,naturalWidth(),naturalHeight(),width,height,NeonHud.gameScale()*Neon.config.hudScale);
        return HudChatDock.active()?HudChatDock.stack(b,HudChatDock.coordinates(width,height,preview)):b;
    }
    public static HudBounds reserved(){return HudWidgets.get(HudKind.CHAT).bounds(mc().getWindow().getWidth(),mc().getWindow().getHeight(),false);}
    public static HudBounds displayed(){
        var b=reserved();boolean preview=mc().gui.screen() instanceof HudEditor;
        if(HudChatDock.active())return preview?b:HudChatDock.place(b);
        if(world!=mc().level||preview){lift.snap(0);world=mc().level;}
        boolean chat=!preview&&mc().gui.screen() instanceof ChatScreen;
        int inputTop=(mc().getWindow().getGuiScaledHeight()-14)*NeonHud.gameScale()-4;
        double amount=lift.to(chat?1:0,System.nanoTime(),220_000_000L);
        int displacement=(int)Math.round(Math.min(b.y(),Math.max(0,b.y()+b.height()-inputTop))*amount);
        return new HudBounds(b.x(),b.y()-displacement,b.width(),b.height());
    }
    public static void transform(Matrix3x2f pose,int screenHeight){
        if(!enabled())return;
        var b=displayed();float factor=NeonHud.gameScale(),fit=scale(),nativeBottom=(float)Math.floor((screenHeight-40)/nativeScale())*nativeScale();
        float bottom=(b.y()+b.height())/factor-bottomPadding()*nativeScale()*fit;
        float padding=Neon.config.betterChat.messageBar?0:2*nativeScale()*fit;
        pose.translate(b.x()/factor+padding,bottom-nativeBottom*fit).scale(fit);
    }
    public static int visibleLines(int original){
        float unit=nativeScale()*scale()*NeonHud.gameScale();
        return Math.min(original,Math.max(0,(int)Math.floor((displayed().height()-bottomPadding()*unit)/(lineHeight()*unit))));
    }
    public static void frame(GuiGraphicsExtractor g,int ticks,ChatComponent.DisplayMode mode,List<GuiMessage.Line> lines,int scroll,int perPage){
        messages=!lines.isEmpty();if(!enabled()||mc().gui.screen() instanceof HudEditor)return;
        int extent=0,count=0;float opacity=0;
        for(int i=0;i<Math.min(lines.size()-scroll,perPage);i++){
            float fade=mode.foreground?1:(float)Math.clamp((1-(ticks-lines.get(i+scroll).addedTime())/200d)*10,0,1);fade*=fade;
            if(fade<=1e-5)continue;extent=i+1;count++;opacity=Math.max(opacity,fade);
        }
        boolean queue=!lines.isEmpty()&&mc().gui.chatListener().queueSize()>0;
        if(mode.showRestrictedPrompt){extent=Math.max(extent,count+1);opacity=1;}if(queue)opacity=1;
        if(opacity<=0)return;
        var b=displayed();float unit=nativeScale()*scale()*NeonHud.gameScale();
        int bottom=Math.round(b.y()+b.height()-bottomPadding()*unit),height=Math.round(extent*lineHeight()*unit);
        var rect=new HudBounds(b.x(),bottom-height,b.width(),height+(queue?Math.round(9*unit):0));
        var c=Neon.config.hud(HudKind.CHAT);var t=Neon.config.theme;
        g.pose().pushMatrix();g.pose().scale(1f/NeonHud.gameScale());
        try{CHROME.panel(g,rect,alpha(t.color(ThemeColor.BACKGROUND),opacity),alpha(t.color(ThemeColor.OUTLINE),opacity),alpha(t.color(ThemeColor.SECONDARY),opacity),c.background,c.border);}finally{g.pose().popMatrix();}
    }
    private static int alpha(int color,float opacity){return color&0xffffff|Math.round((color>>>24)*opacity)<<24;}
    @Override public void surface(GuiGraphicsExtractor g,int width,int height,boolean preview){if(preview&&enabled()){var c=Neon.config.hud(kind);CHROME.panel(g,bounds(width,height,true),c.background,c.border);}}
    @Override public void draw(GuiGraphicsExtractor g,int width,int height,boolean preview){
        if(!preview||!enabled())return;
        var b=bounds(width,height,true);
        g.pose().pushMatrix();g.pose().scale(NeonHud.gameScale());
        try{mc().gui.hud.getChat().extractRenderState(g,mc().font,mc().gui.hud.getGuiTicks(),-1,-1,ChatComponent.DisplayMode.FOREGROUND,false);}finally{g.pose().popMatrix();}
        if(!messages){g.pose().pushMatrix();g.pose().translate(b.x()+4,b.y()+b.height()-24);g.pose().scale(2);g.text(mc().font,Component.literal("Chat messages appear here").withStyle(ChatText::style),0,0,0xffdddddd);g.pose().popMatrix();}
    }
}

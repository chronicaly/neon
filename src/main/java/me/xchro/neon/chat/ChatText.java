package me.xchro.neon.chat;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.ui.HudText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.font.EmptyArea;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/** Unity's chat layout/click-area bridge, adapted to Neon's installed-font atlas. */
public final class ChatText {
    private static final FontDescription.Resource FONT=new FontDescription.Resource(Identifier.parse("neon:chat"));
    private static final Map<Integer,String> GLYPHS=new LinkedHashMap<>(256,.75f,true){protected boolean removeEldestEntry(Map.Entry<Integer,String> e){return size()>1024;}};
    private static final Map<FormattedCharSequence,Layout> LAYOUTS=new LinkedHashMap<>(128,.75f,true){protected boolean removeEldestEntry(Map.Entry<FormattedCharSequence,Layout> e){return size()>128;}};
    private static String family;
    private record Glyph(String text,Style style,float x,float width){}
    private static final class Layout {
        final List<Glyph> glyphs;
        final float width;
        float preparedX,preparedY;
        Font.PreparedText prepared;
        Layout(List<Glyph> glyphs,float width){this.glyphs=glyphs;this.width=width;}
    }
    private ChatText(){}
    public static boolean enabled(){return BetterChat.active()&&Neon.config.betterChat.clientFont;}
    public static Style style(Style style){return enabled()?style.withFont(FONT):style;}
    public static boolean marked(Style style){return enabled()&&FONT.equals(style.getFont());}
    public static boolean uses(FormattedCharSequence text){if(!enabled())return false;boolean[] found={false};text.accept((i,s,cp)->{if(marked(s)){found[0]=true;return false;}return true;});return found[0];}
    private static String glyph(int cp){return GLYPHS.computeIfAbsent(cp,k->new String(Character.toChars(k)));}
    public static float advance(int cp,Style style){return HudText.advance(glyph(cp),64,style.isBold(),style.isItalic())/8f;}
    private static Layout layout(FormattedCharSequence text){
        if(!Objects.equals(family,Neon.config.clientFont)){family=Neon.config.clientFont;LAYOUTS.clear();}
        return LAYOUTS.computeIfAbsent(text,key->{var glyphs=new ArrayList<Glyph>();float[] x={0};key.accept((i,s,cp)->{float width=advance(cp,s);glyphs.add(new Glyph(glyph(cp),s,x[0],width));x[0]+=width;return true;});return new Layout(glyphs,x[0]);});
    }
    public static float width(FormattedCharSequence text){return layout(text).width;}
    public static Style at(FormattedCharSequence text,float mouseX){for(var glyph:layout(text).glyphs)if(mouseX>=glyph.x&&mouseX<glyph.x+glyph.width)return glyph.style;return null;}
    public static Font.PreparedText areas(FormattedCharSequence text,float x,float y){
        var layout=layout(text);
        if(layout.prepared!=null&&layout.preparedX==x&&layout.preparedY==y)return layout.prepared;
        var areas=new ArrayList<EmptyArea>();Style style=null;float start=0,end=0;
        // Native pointer collection needs style spans, not one allocated rectangle per letter.
        for(var glyph:layout.glyphs){
            if(style!=null&&!style.equals(glyph.style)){areas.add(new EmptyArea(x+start,y,end-start,7,9,style));style=null;}
            if(style==null){style=glyph.style;start=glyph.x;}end=glyph.x+glyph.width;
        }
        if(style!=null)areas.add(new EmptyArea(x+start,y,end-start,7,9,style));
        var bounds=new ScreenRectangle((int)Math.floor(x),(int)Math.floor(y),(int)Math.ceil(layout.width),9);
        layout.preparedX=x;layout.preparedY=y;
        return layout.prepared=new Font.PreparedText(){public void visit(Font.GlyphVisitor v){for(var area:areas)v.acceptEmptyArea(area);}public ScreenRectangle bounds(){return bounds;}};
    }
    public static void draw(GuiGraphicsExtractor g,FormattedCharSequence text,float x,float y,float opacity){
        float physical=Math.abs(g.pose().m11())*Minecraft.getInstance().getWindow().getGuiScale();
        int pixels=Math.clamp(Math.round(8*physical),1,96),baseline=Math.round(7*physical);
        boolean shadow=!me.xchro.neon.hud.ChatHud.enabled()||Neon.config.hud(me.xchro.neon.hud.HudKind.CHAT).shadow;
        // Unity finishes all shadows before any foreground, including across color/link boundaries.
        if(shadow){g.pose().pushMatrix();g.pose().translate(1/Math.max(.01f,physical),1/Math.max(.01f,physical));layer(g,text,x,y,opacity*.7f,pixels,baseline,true);g.pose().popMatrix();g.guiRenderState.up();}
        layer(g,text,x,y,opacity,pixels,baseline,false);
    }
    private static void layer(GuiGraphicsExtractor g,FormattedCharSequence text,float x,float y,float opacity,int pixels,int baseline,boolean shadow){
        var layout=layout(text);
        try(var batch=HudText.batch(g,layout.glyphs.size())){
            for(var glyph:layout.glyphs){var s=glyph.style;int color=(Math.clamp(Math.round(opacity*255),0,255)<<24)|(shadow?0:s.getColor()==null?0xffffff:s.getColor().getValue());
                g.pose().pushMatrix();g.pose().translate(x+glyph.x,y);HudText.draw(g,glyph.text,0,0,color,pixels,false,s.isBold(),s.isItalic(),baseline);g.pose().popMatrix();
            }
        }
        if(!shadow){decorations(g,layout,x,y,opacity,true);decorations(g,layout,x,y,opacity,false);}
    }
    private static void decorations(GuiGraphicsExtractor g,Layout layout,float x,float y,float opacity,boolean underline){
        boolean open=false;float start=0,end=0;int color=0,row=(int)y+(underline?8:4);
        for(var glyph:layout.glyphs){var s=glyph.style;boolean visible=underline?s.isUnderlined():s.isStrikethrough();
            int next=(Math.clamp(Math.round(opacity*255),0,255)<<24)|(s.getColor()==null?0xffffff:s.getColor().getValue());
            if(open&&(!visible||next!=color)){g.fill((int)(x+start),row,(int)Math.ceil(x+end),row+1,color);open=false;}
            if(visible){if(!open){start=glyph.x;color=next;open=true;}end=glyph.x+glyph.width;}
        }
        if(open)g.fill((int)(x+start),row,(int)Math.ceil(x+end),row+1,color);
    }
}

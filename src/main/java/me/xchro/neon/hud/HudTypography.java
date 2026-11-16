package me.xchro.neon.hud;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.ui.HudText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/** Unity's physical font sizing and snapped bitmap origins, adapted to native-resolution Neon Inter. */
public final class HudTypography {
    private double scale=2;
    private int pixels=16;
    private double bitmapScale=2;
    private final LinkedHashMap<FormattedCharSequence,List<Run>> styled=new LinkedHashMap<>(32,.75f,true);
    private final LinkedHashMap<String,FormattedCharSequence> legacy=new LinkedHashMap<>(32,.75f,true);
    private record Run(String text,Style style) {}
    public void scale(double value) { scale=Math.max(.01,value);pixels=Math.clamp((int)Math.round(8*scale),1,96);bitmapScale=Math.max(1,Math.round(scale)); }
    private boolean inter() { return Neon.config.hudFont==me.xchro.neon.config.NeonConfig.HudFont.INTER; }
    public int height() { return (int)Math.ceil(inter()?pixels*1.2/scale:Minecraft.getInstance().font.lineHeight*bitmapScale/scale); }
    private FormattedCharSequence formatted(String value) {
        var found=legacy.get(value);if(found!=null)return found;
        FormattedCharSequence sequence=sink->net.minecraft.util.StringDecomposer.iterateFormatted(value,Style.EMPTY,sink);
        if(legacy.size()>=64)legacy.remove(legacy.keySet().iterator().next());legacy.put(value,sequence);return sequence;
    }
    public int width(String text) {
        if(text.indexOf('§')>=0)return width(formatted(text));
        return (int)Math.ceil(inter()?HudText.width(text,pixels)/scale:Minecraft.getInstance().font.width(text)*bitmapScale/scale);
    }
    public String trim(String text,int limit) {
        if(width(text)<=limit)return text;
        int count=text.codePointCount(0,text.length()),lo=0,hi=count;
        while(lo<hi) { int mid=(lo+hi+1)/2;if(width(text.substring(0,text.offsetByCodePoints(0,mid))+"…")<=limit)lo=mid;else hi=mid-1; }
        return lo==0?"":text.substring(0,text.offsetByCodePoints(0,lo))+"…";
    }
    public void draw(GuiGraphicsExtractor g,String text,int x,int y,int color,boolean shadow) {
        if(text.indexOf('§')>=0) { draw(g,formatted(text),x,y,color,shadow);return; }
        if(inter())HudText.draw(g,text,x,y,color,pixels,shadow);
        else {
            g.pose().pushMatrix();g.pose().translate(x,y).scale((float)(bitmapScale/scale));
            MinecraftFontLayout.snapOrigin(g.pose(),Minecraft.getInstance().getWindow().getGuiScale());
            g.text(Minecraft.getInstance().font,text,0,0,color,shadow);g.pose().popMatrix();
        }
    }
    private List<Run> runs(FormattedCharSequence sequence) {
        var found=styled.get(sequence);if(found!=null)return found;
        var result=new ArrayList<Run>();var buffer=new StringBuilder();Style[] previous={null};
        sequence.accept((index,style,codepoint)->{
            if(!style.equals(previous[0])&&buffer.length()>0){result.add(new Run(buffer.toString(),previous[0]));buffer.setLength(0);}
            previous[0]=style;buffer.appendCodePoint(codepoint);return true;
        });
        if(buffer.length()>0)result.add(new Run(buffer.toString(),previous[0]));
        if(styled.size()>=64)styled.remove(styled.keySet().iterator().next());styled.put(sequence,result);return result;
    }
    public int width(FormattedCharSequence sequence) {
        if(!inter(sequence))return (int)Math.ceil(Minecraft.getInstance().font.width(sequence)*bitmapScale/scale);
        int width=0;for(var run:runs(sequence))width+=HudText.width(run.text,pixels,run.style.isBold(),run.style.isItalic());
        return (int)Math.ceil(width/scale);
    }
    public void draw(GuiGraphicsExtractor g,FormattedCharSequence sequence,int x,int y,int color,boolean shadow) {
        if(!inter(sequence)) {
            g.pose().pushMatrix();g.pose().translate(x,y).scale((float)(bitmapScale/scale));MinecraftFontLayout.snapOrigin(g.pose(),Minecraft.getInstance().getWindow().getGuiScale());
            g.text(Minecraft.getInstance().font,sequence,0,0,color,shadow);g.pose().popMatrix();return;
        }
        double advance=0;
        for(var run:runs(sequence)) {
            int tint=run.style.getColor()==null?color:(color&0xff000000)|run.style.getColor().getValue();
            g.pose().pushMatrix();g.pose().translate((float)advance,0);
            HudText.draw(g,run.text,x,y,tint,pixels,shadow,run.style.isBold(),run.style.isItalic());
            double w=HudText.width(run.text,pixels,run.style.isBold(),run.style.isItalic())/scale;
            if(run.style.isUnderlined())g.fill(x,y+height()-1,x+(int)Math.ceil(w),y+height(),tint);
            if(run.style.isStrikethrough())g.fill(x,y+height()/2,x+(int)Math.ceil(w),y+height()/2+1,tint);
            g.pose().popMatrix();advance+=w;
        }
    }
    private boolean inter(FormattedCharSequence sequence) {
        if(!inter())return false;
        // Resource-pack icon fonts and animated obfuscation retain Minecraft's own renderer.
        for(var run:runs(sequence))if(run.style.isObfuscated()||!run.style.getFont().equals(Style.EMPTY.getFont()))return false;
        return true;
    }
}

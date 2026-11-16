package me.xchro.neon.core;

import java.util.ArrayList;
import java.util.List;

/** Cached pixel coverage: the same thin, symmetric geometry at every GUI scale. */
public final class CrosshairRaster {
    public enum Shape { CROSS, T, DOT, SQUARE }
    public record Key(Shape shape, float length, float gap, float width, boolean dot, float outline, boolean oddX, boolean oddY) {}
    public record Run(int x, int y, int length, int fill, int border) {
        public int color(int argb) {
            int coverage=fill+border;
            if (coverage==0) return 0;
            int alpha=Math.round((argb>>>24)*coverage/64f);
            int r=(argb>>16&255)*fill/coverage, g=(argb>>8&255)*fill/coverage, b=(argb&255)*fill/coverage;
            return alpha<<24|r<<16|g<<8|b;
        }
    }
    private record Rect(double x0,double y0,double x1,double y1) {
        boolean contains(double x,double y,double expand) {return x>=x0-expand&&x<x1+expand&&y>=y0-expand&&y<y1+expand;}
    }
    private CrosshairRaster() {}
    public static List<Run> rasterize(Key key) {
        double half=key.width/2, gap=key.gap, length=key.length;
        List<Rect> rects=new ArrayList<>();
        if(key.shape==Shape.CROSS||key.shape==Shape.T){
            rects.add(new Rect(-gap-length,-half,-gap,half));
            rects.add(new Rect(gap,-half,gap+length,half));
            rects.add(new Rect(-half,gap,half,gap+length));
            if(key.shape==Shape.CROSS)rects.add(new Rect(-half,-gap-length,half,-gap));
        }else if(key.shape==Shape.SQUARE){
            double s=gap+length;
            rects.add(new Rect(-s-half,-s-half,s+half,-s+half));
            rects.add(new Rect(-s-half,s-half,s+half,s+half));
            rects.add(new Rect(-s-half,-s+half,-s+half,s-half));
            rects.add(new Rect(s-half,-s+half,s+half,s-half));
        }
        if(key.dot||key.shape==Shape.DOT)rects.add(new Rect(-half,-half,half,half));
        int radius=(int)Math.ceil(gap+length+half+key.outline+1);
        double centerX=key.oddX?.5:0,centerY=key.oddY?.5:0;
        List<Run> runs=new ArrayList<>();
        for(int y=-radius;y<=radius;y++){
            Run run=null;
            for(int x=-radius;x<=radius;x++){
                int fill=0,border=0;
                for(int sy=0;sy<8;sy++)for(int sx=0;sx<8;sx++){
                    double px=x+(sx+.5)/8-centerX,py=y+(sy+.5)/8-centerY;
                    boolean inside=false,outer=false;
                    for(Rect rect:rects){inside|=rect.contains(px,py,0);outer|=rect.contains(px,py,key.outline);}
                    if(inside)fill++;else if(outer)border++;
                }
                if(run!=null&&run.fill==fill&&run.border==border){run=new Run(run.x,y,run.length+1,fill,border);}
                else {if(run!=null&&run.fill+run.border>0)runs.add(run);run=new Run(x,y,1,fill,border);}
            }
            if(run!=null&&run.fill+run.border>0)runs.add(run);
        }
        return List.copyOf(runs);
    }
}


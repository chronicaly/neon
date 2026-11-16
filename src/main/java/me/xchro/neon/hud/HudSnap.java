package me.xchro.neon.hud;

import java.util.List;

/** Alignment in physical display pixels; a guide is the aligned edge/center, not the widget origin. */
public final class HudSnap {
    private static final int GAP = 6;
    public record Result(double x, double y, double guideX, double guideY) {}
    private record Axis(double position, double guide) {}
    private HudSnap() {}

    /** One drag owns its edge magnets. Continuing past a caught edge deliberately releases it. */
    public static final class Drag {
        private final Magnet horizontal=new Magnet(),vertical=new Magnet();
        public Result move(HudBounds moving,List<HudBounds> others,int width,int height,double x,double y,int sensitivity,boolean align) {
            x=Math.clamp(x,0,Math.max(0,width-moving.width()));y=Math.clamp(y,0,Math.max(0,height-moving.height()));
            Result aligned=align?HudSnap.move(moving,others,width,height,x,y,sensitivity):new Result(x,y,-1,-1);
            Axis hx=horizontal.move(moving,others,x,y,sensitivity,true);
            Axis vy=vertical.move(moving,others,y,x,sensitivity,false);
            if(!align&&horizontal.target!=null&&vy==null)vy=axis(y,x,moving.height(),moving.width(),height,List.of(horizontal.target),sensitivity,false);
            if(!align&&vertical.target!=null&&hx==null)hx=axis(x,y,moving.width(),moving.height(),width,List.of(vertical.target),sensitivity,true);
            return new Result(hx==null?aligned.x:hx.position,vy==null?aligned.y:vy.position,
                hx==null?aligned.guideX:hx.guide,vy==null?aligned.guideY:vy.guide);
        }
        public void clear(){horizontal.clear();vertical.clear();}
    }
    private static final class Magnet {
        private HudBounds target,released;
        private double edge,captured;
        private int direction;
        void clear(){target=released=null;}
        Axis move(HudBounds moving,List<HudBounds> others,double desired,double cross,int sensitivity,boolean horizontal) {
            int size=horizontal?moving.width():moving.height(),crossSize=horizontal?moving.height():moving.width();
            double current=horizontal?moving.x():moving.y(),oldCross=horizontal?moving.y():moving.x();
            int threshold=Math.max(18,sensitivity*2+GAP);
            if(released!=null&&(!others.contains(released)||separate(desired,cross,size,crossSize,released,horizontal)))released=null;
            if(target!=null) {
                if(!others.contains(target)||!crosses(cross,crossSize,target,horizontal)||direction*(desired-edge)<-sensitivity)target=null;
                else if(direction*(desired-captured)>threshold){released=target;target=null;return new Axis(desired,-1);}
                else return new Axis(edge,direction>0?edge+size:edge);
            }
            double nearest=Double.POSITIVE_INFINITY;HudBounds found=null;double foundEdge=0;int foundDirection=0;
            for(var other:others) {
                if(other.equals(released))continue;
                double start=horizontal?other.x():other.y(),length=horizontal?other.width():other.height();
                double before=start-size-GAP,after=start+length+GAP;
                int dir=desired>current?1:desired<current?-1:0;
                double stop=dir>0?before:after;
                if(dir==0||dir*(current-stop)>.01||dir*(desired-stop)<-sensitivity)continue;
                double travel=desired-current,t=travel==0?0:Math.clamp((stop-current)/travel,0,1);
                double atCross=oldCross+(cross-oldCross)*t;
                if(!crosses(atCross,crossSize,other,horizontal))continue;
                double distance=Math.abs(stop-current);
                if(distance<nearest){nearest=distance;found=other;foundEdge=stop;foundDirection=dir;}
            }
            if(found!=null){target=found;edge=foundEdge;direction=foundDirection;captured=desired;return new Axis(edge,direction>0?edge+size:edge);}
            // Do not let ordinary center/edge alignment pull a deliberately overlapped widget back out.
            return released==null?null:new Axis(desired,-1);
        }
        private static boolean crosses(double position,int size,HudBounds other,boolean horizontal){
            int start=horizontal?other.y():other.x(),length=horizontal?other.height():other.width();
            return position<start+length&&position+size>start;
        }
        private static boolean separate(double position,double cross,int size,int crossSize,HudBounds other,boolean horizontal){
            int start=horizontal?other.x():other.y(),length=horizontal?other.width():other.height();
            return !crosses(cross,crossSize,other,horizontal)||position+size<start-GAP||position>start+length+GAP;
        }
    }

    public static Result move(HudBounds moving, List<HudBounds> visibleOthers, int width, int height,
                              double x, double y, int sensitivity) {
        x = Math.clamp(x, 0, Math.max(0, width - moving.width()));
        y = Math.clamp(y, 0, Math.max(0, height - moving.height()));
        Axis horizontal = axis(x, y, moving.width(), moving.height(), width, visibleOthers, sensitivity, true);
        Axis vertical = axis(y, x, moving.height(), moving.width(), height, visibleOthers, sensitivity, false);
        return new Result(horizontal.position, vertical.position, horizontal.guide, vertical.guide);
    }

    private static Axis axis(double position, double perpendicular, int size, int perpendicularSize, int extent,
                             List<HudBounds> others, int sensitivity, boolean horizontal) {
        var candidates = new java.util.ArrayList<Axis>();
        for (double target : new double[]{0, extent / 2.0, extent})
            align(candidates, target, size);
        for (HudBounds other : others) {
            int start = horizontal ? other.x() : other.y(), length = horizontal ? other.width() : other.height();
            for (double target : new double[]{start, start + length / 2.0, start + length}) align(candidates, target, size);
            int cross = horizontal ? other.y() : other.x(), crossSize = horizontal ? other.height() : other.width();
            if (perpendicular <= cross + crossSize + GAP && perpendicular + perpendicularSize >= cross - GAP) {
                candidates.add(new Axis(start - GAP - size, start - GAP));
                candidates.add(new Axis(start + length + GAP, start + length + GAP));
            }
        }
        Axis result = new Axis(position, -1);
        double nearest = Math.max(0, sensitivity) + .0001;
        for (Axis candidate : candidates) {
            if (candidate.position < 0 || candidate.position > extent - size) continue;
            double distance = Math.abs(candidate.position - position);
            if (distance < nearest) { nearest = distance; result = candidate; }
        }
        return result;
    }
    private static void align(List<Axis> candidates, double target, int size) {
        for (double anchor : new double[]{0, size / 2.0, size}) candidates.add(new Axis(target - anchor, target));
    }
}

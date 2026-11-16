package me.xchro.neon.hud;

import java.awt.BasicStroke;
import java.awt.geom.*;
import java.util.*;

/** Cached physical-pixel union: close small gaps, then restore the original outside edge. */
public final class HudGlassGeometry {
    public record Box(int x,int y,int width,int height) {}
    public record Quad(float leftTop,float rightTop,float top,float leftBottom,float rightBottom,float bottom) {}
    public record Edge(float x0,float y0,float x1,float y1,float nx0,float ny0,float nx1,float ny1) {}
    public record Mesh(List<Quad> fill,List<Edge> edge,Rectangle2D bounds) {}
    private record Segment(Point2D.Float a,Point2D.Float b) {
        float x(double y) { return (float)(a.x+(b.x-a.x)*(y-a.y)/(b.y-a.y)); }
    }
    private HudGlassGeometry() {}
    public static Area shape(List<Box> boxes) {
        Area result=new Area();
        for(var b:boxes)if(b.width>0&&b.height>0) {
            float radius=Math.min(3,Math.min(b.width,b.height)/4f);
            result.add(new Area(new RoundRectangle2D.Float(b.x,b.y,b.width,b.height,radius*2,radius*2)));
        }
        if(boxes.size()>1&&!result.isEmpty()) {
            var bridge=new BasicStroke(8,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
            result.add(new Area(bridge.createStrokedShape(result)));
            result.subtract(new Area(bridge.createStrokedShape(result)));
        }
        return result;
    }
    public static Mesh build(List<Box> boxes) {
        Area area=shape(boxes);var contours=new ArrayList<List<Point2D.Float>>();
        List<Point2D.Float> current=null;float[] point=new float[6];
        for(var path=area.getPathIterator(null,.18);!path.isDone();path.next()) {
            int type=path.currentSegment(point);
            if(type==PathIterator.SEG_MOVETO){current=new ArrayList<>();contours.add(current);current.add(new Point2D.Float(point[0],point[1]));}
            else if(type==PathIterator.SEG_LINETO) {
                var p=new Point2D.Float(point[0],point[1]);if(!p.equals(current.getLast()))current.add(p);
            }
        }
        var segments=new ArrayList<Segment>();var edges=new ArrayList<Edge>();var heights=new TreeSet<Float>();
        var columns=new TreeSet<Float>();var rows=new TreeSet<Float>();
        // Keep local color overrides inside their widget even when the contour is shared.
        for(var box:boxes){columns.add((float)box.x);columns.add((float)(box.x+box.width));rows.add((float)box.y);rows.add((float)(box.y+box.height));}
        for(var points:contours) {
            if(points.size()>1&&points.getFirst().equals(points.getLast()))points.removeLast();
            int count=points.size();if(count<3)continue;
            float[] nx=new float[count],ny=new float[count];
            for(int i=0;i<count;i++) {
                var a=points.get(i);var b=points.get((i+1)%count);float dx=b.x-a.x,dy=b.y-a.y,length=(float)Math.hypot(dx,dy);
                nx[i]=-dy/length;ny[i]=dx/length;
                if(!area.contains((a.x+b.x)*.5+nx[i]*.5,(a.y+b.y)*.5+ny[i]*.5)){nx[i]=-nx[i];ny[i]=-ny[i];}
                segments.add(new Segment(a,b));heights.add(a.y);
            }
            float[] mx=new float[count],my=new float[count];
            for(int i=0;i<count;i++) {
                int before=(i+count-1)%count;float x=nx[before]+nx[i],y=ny[before]+ny[i],den=x*nx[i]+y*ny[i];
                if(Math.abs(den)<.1f){mx[i]=nx[i];my[i]=ny[i];}else{mx[i]=x/den;my[i]=y/den;}
            }
            for(int i=0;i<count;i++) {
                int next=(i+1)%count;var a=points.get(i);var b=points.get(next);
                var cuts=new TreeSet<Float>();cuts.add(0f);cuts.add(1f);
                for(float x:columns)if(b.x!=a.x){float t=(x-a.x)/(b.x-a.x);if(t>0&&t<1)cuts.add(t);}
                for(float y:rows)if(b.y!=a.y){float t=(y-a.y)/(b.y-a.y);if(t>0&&t<1)cuts.add(t);}
                float last=0;
                for(float t:cuts){if(t>last)edges.add(new Edge(a.x+(b.x-a.x)*last,a.y+(b.y-a.y)*last,a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t,
                    mx[i]+(mx[next]-mx[i])*last,my[i]+(my[next]-my[i])*last,mx[i]+(mx[next]-mx[i])*t,my[i]+(my[next]-my[i])*t));last=t;}
            }
        }
        heights.addAll(rows);
        for(var edge:segments)for(float x:columns)if(edge.a.x!=edge.b.x){
            float t=(x-edge.a.x)/(edge.b.x-edge.a.x);if(t>0&&t<1)heights.add(edge.a.y+(edge.b.y-edge.a.y)*t);
        }
        var fills=new ArrayList<Quad>();var levels=new ArrayList<>(heights);
        for(int row=1;row<levels.size();row++) {
            float top=levels.get(row-1),bottom=levels.get(row);if(bottom-top<.0001)continue;double mid=(top+bottom)*.5;
            var crossings=new ArrayList<Segment>();
            for(var edge:segments)if(mid>Math.min(edge.a.y,edge.b.y)&&mid<Math.max(edge.a.y,edge.b.y))crossings.add(edge);
            crossings.sort(Comparator.comparingDouble(edge->edge.x(mid)));
            for(int i=1;i<crossings.size();i+=2) {
                var left=crossings.get(i-1);var right=crossings.get(i);
                float lt=left.x(top),lb=left.x(bottom),rt=right.x(top),rb=right.x(bottom);
                for(float x:columns)if(x>left.x(mid)&&x<right.x(mid)){
                    fills.add(new Quad(lt,x,top,lb,x,bottom));lt=lb=x;
                }
                fills.add(new Quad(lt,rt,top,lb,rb,bottom));
            }
        }
        return new Mesh(List.copyOf(fills),List.copyOf(edges),area.getBounds2D());
    }
}

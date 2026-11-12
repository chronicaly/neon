package me.xchro.neon.core;

/** A highlight travels in display pixels, independent of the changing rectangle's center. */
public final class OutlineTravel {
    public static final double SPEED = 160; // display pixels per second
    private boolean ready;
    private int x,y,w,h;
    private double distance;
    private long previous;
    public record Point(double x,double y,int edge) {}
    public double update(int x,int y,int w,int h,long now) {
        if(!ready) {ready=true;this.x=x;this.y=y;this.w=w;this.h=h;previous=now;}
        distance=wrap(distance+Math.max(0,now-previous)/1e9*SPEED,2.0*(this.w+this.h));
        previous=now;
        if(w!=this.w||h!=this.h) {
            Point p=point();
            // Preserve the actual position along this edge. Only its normal follows a moving edge.
            distance=switch(p.edge) {
                case 0 -> Math.clamp(p.x-x,0,w);
                case 1 -> w+Math.clamp(p.y-y,0,h);
                case 2 -> w+h+Math.clamp(x+w-p.x,0,w);
                default -> 2*w+h+Math.clamp(y+h-p.y,0,h);
            };
        }
        this.x=x;this.y=y;this.w=w;this.h=h;
        distance=wrap(distance,2.0*(w+h));
        return distance;
    }
    public Point point() {
        double d=distance;
        if(d<w)return new Point(x+d,y,0);d-=w;
        if(d<h)return new Point(x+w,y+d,1);d-=h;
        if(d<w)return new Point(x+w-d,y+h,2);d-=w;
        return new Point(x,y+h-d,3);
    }
    private static double wrap(double n,double perimeter){return n%perimeter;}
}

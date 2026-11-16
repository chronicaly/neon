package me.xchro.neon.core;

/** Bounded, allocation-free trailing second of physical button presses. */
public final class ClickWindow {
    private final long[] times=new long[512];
    private int first,size;
    private long previous;
    private void expire(long now) {
        if(now<previous)clear();previous=now;
        while(size>0 && now-times[first]>=1_000_000_000L) { first=(first+1)%times.length;size--; }
    }
    public void press(long now) {
        expire(now);
        if(size==times.length) { first=(first+1)%times.length;size--; }
        times[(first+size)%times.length]=now;size++;
    }
    public int count(long now) { expire(now);return size; }
    public void clear() { first=size=0;previous=0; }
}

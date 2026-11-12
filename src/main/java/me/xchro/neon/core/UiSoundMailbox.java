package me.xchro.neon.core;

import me.xchro.neon.core.UiSoundPolicy.Cue;
import me.xchro.neon.core.UiSoundPolicy.Request;

/** Two bounded slots: rapid input replaces pending audio instead of building a backlog. */
public final class UiSoundMailbox {
    private Request action, hover;
    public synchronized void offer(Request request) {
        if (request.cue() == Cue.HOVER) hover = request;
        else { action = request; hover = null; }
    }
    public synchronized Request poll(long now) {
        Request next = action;
        if (next != null) action = null;
        else { next = hover; hover = null; }
        if (next != null && now - next.time() > 150_000_000L) return poll(now);
        return next;
    }
    public synchronized boolean empty() { return action == null && hover == null; }
    public synchronized void clear() { action = hover = null; }
}

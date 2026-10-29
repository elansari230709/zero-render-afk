package com.ayoubela.zerorenderafk.client.antiafk;

/** Monotonic clock: no burst or replay of missed actions after a stalled tick. */
public final class ActionClock {
    private final long period;
    private long deadline;
    public ActionClock(double seconds, long now) {
        period = Math.round(seconds * 1_000_000_000D);
        postpone(now);
    }
    public boolean due(long now) {
        if (now - deadline < 0) return false;
        postpone(now);
        return true;
    }
    public void postpone(long now) { deadline = now + period; }
}

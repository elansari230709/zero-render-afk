package com.ayoubela.zerorenderafk.client.antiafk;

import java.util.ArrayList;
import java.util.List;

/** Experimental flat-ground trajectory; not a collision or potion simulator. */
public final class JumpTrajectory {
    private final double[] offsets;
    private int index;
    public JumpTrajectory(double height) {
        List<Double> samples = new ArrayList<>();
        double y = 0, velocity = 0.42, peak = 0;
        for (int tick = 0; tick < 40; tick++) {
            y += velocity;
            if (y <= 0) { samples.add(0D); break; }
            samples.add(y); peak = Math.max(peak, y);
            velocity = (velocity - 0.08) * 0.98;
        }
        offsets = new double[samples.size()];
        for (int i = 0; i < offsets.length; i++) offsets[i] = samples.get(i) * height / peak;
    }
    public double nextOffset() { return offsets[index++]; }
    public boolean finished() { return index >= offsets.length; }
}

package mobi.ioio.plotter.shapes;

import mobi.ioio.plotter.Curve;

public class Bezier extends Curve {
    final float x1_, y1_, x2_, y2_, x3_, y3_;
    final double approxLength_;

    public Bezier(float x1, float y1, float x2, float y2, float x3, float y3) {
        x1_ = x1;
        y1_ = y1;
        x2_ = x2;
        y2_ = y2;
        x3_ = x3;
        y3_ = y3;
        approxLength_ = Math.hypot(x2 - x1, y2 - y1) + Math.hypot(x3 - x2, y3 - y2);
    }

    @Override
    public double totalTime() {
        return approxLength_;
    }

    @Override
    public void getPosTime(double time, float[] xy) {
        double t = time / approxLength_;
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        xy[0] = (float) ((1 - t) * (1 - t) * x1_ + 2 * (1 - t) * t * x2_ + t * t * x3_);
        xy[1] = (float) ((1 - t) * (1 - t) * y1_ + 2 * (1 - t) * t * y2_ + t * t * y3_);
    }

    @Override
    public float[] getBounds() {
        return new float[] {
                Math.min(Math.min(x1_, x2_), x3_),
                Math.min(Math.min(y1_, y2_), y3_),
                Math.min(Math.max(x1_, x2_), x3_),
                Math.min(Math.max(y1_, y2_), y3_)
        };
    }
}

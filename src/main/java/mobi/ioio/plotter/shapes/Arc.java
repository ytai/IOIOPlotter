package mobi.ioio.plotter.shapes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mobi.ioio.plotter.Curve;

public class Arc extends Curve {
    final float centerx_;
    final float centery_;
    final float radius_;
    final float startAngle_;
    final float endAngle_;
    final float totalLength_;

    public Arc(float centerx, float centery, float radius, float startAngle, float endAngle) {
        centerx_ = centerx;
        centery_ = centery;
        startAngle_ = startAngle;
        endAngle_ = endAngle;
        radius_ = radius;
        totalLength_ = radius * Math.abs(endAngle - startAngle);
    }

    @Override
    public double totalTime() {
        return totalLength_;
    }

    @Override
    public void getPosTime(double time, float[] xy) {
        float t = (float) (time / totalLength_);
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        float angle = startAngle_ + (endAngle_ - startAngle_) * t;
        xy[0] = (float) (centerx_ + radius_ * Math.cos(angle));
        xy[1] = (float) (centery_ - radius_ * Math.sin(angle));
    }

    @Override
    public float[] getBounds() {
        final float step = (float) Math.PI / 2;
        // Check the bounds at the start and end angle, as well as any integer multiples of PI/2 in
        // between.
        List<Float> angles = new ArrayList<Float>(2);
        angles.add(startAngle_);
        angles.add(endAngle_);

        if (startAngle_ < endAngle_) {
            for (float angle = (float) Math.ceil(startAngle_ / step) * step; angle < endAngle_; angle += step) {
                angles.add(angle);
            }
        } else {
            for (float angle = (float) Math.floor(startAngle_ / step) * step; angle > endAngle_; angle -= step) {
                angles.add(angle);
            }
        }

        float[] bounds = new float[]{Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};

        for (float angle : angles) {
            float x = (float) (centerx_ + radius_ * Math.cos(angle));
            float y = (float) (centery_ - radius_ * Math.sin(angle));
            bounds[0] = Math.min(bounds[0], x);
            bounds[1] = Math.min(bounds[1], y);
            bounds[2] = Math.max(bounds[2], x);
            bounds[3] = Math.max(bounds[3], y);
        }

        return bounds;
    }
}

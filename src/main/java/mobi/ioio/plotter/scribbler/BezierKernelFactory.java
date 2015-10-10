package mobi.ioio.plotter.scribbler;

import java.util.Random;

import mobi.ioio.plotter.Curve;
import mobi.ioio.plotter.shapes.Bezier;
import mobi.ioio.plotter.shapes.Line;
import mobi.ioio.plotter.shapes.SingleCurveMultiCurve;

public class BezierKernelFactory implements KernelFactory {
    private float width_;
    private float height_;
    private final Random random_ = new Random();

    @Override
    public void setDimensions(float width, float height) {
        width_ = width;
        height_ = height;
    }

    @Override
    public KernelInstance createKernelInstance(Object context) {
        float[] history = (float[]) context;
        if (history == null) {
            history = new float[] {
                    random_.nextFloat() * width_, random_.nextFloat() * height_,
                    random_.nextFloat() * width_, random_.nextFloat() * height_
            };
        }

        float x1 = history[0];
        float y1 = history[1];
        float x2 = history[2];
        float y2 = history[3];

        // Find where the ray starting at <x1, y1> and passing through <x2, y2>
        // intersects with any border.
        float[] intersection = GeometryUtil.intersectLineWithBorders(
                width_, height_, x1, y1, (float) -Math.atan2(y2 - y1, x2 - x1));

        // Find a random point on the line between point 2 and the intersection with the border.
        float dist1To2 = (float) Math.hypot(x2 - x1, y2 - y1);
        float dist1To3 = intersection[4];
        float ratio = (dist1To2 + random_.nextFloat() * (dist1To3 - dist1To2)) / dist1To2;
        float x3 = x1 + (x2 - x1) * ratio;
        float y3 = y1 + (y2 - y1) * ratio;

        // And another random point.
        float x4 = random_.nextFloat() * width_;
        float y4 = random_.nextFloat() * height_;

        // Create a Bezier curve from point 2 to point 4 with a handle at point 3.
        Curve bezier = new Bezier(x2, y2, x3, y3, x4, y4);

        // Create the context for the next curve.
        float[] nextContext = new float[] { x3, y3, x4, y4 };

        return new KernelInstance(new SingleCurveMultiCurve(bezier), nextContext);
    }
}

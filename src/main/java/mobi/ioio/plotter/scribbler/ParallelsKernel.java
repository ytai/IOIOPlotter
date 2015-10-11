package mobi.ioio.plotter.scribbler;

import java.util.Random;

import mobi.ioio.plotter.shapes.Line;
import mobi.ioio.plotter.shapes.SingleCurveMultiCurve;

public class ParallelsKernel implements ConstrainedCurveKernel {
    private final Random random_ = new Random();
    private float width_;
    private float height_;
    private final float angle_;

    public ParallelsKernel(float angle) {
        angle_ = angle;
    }

    @Override
    public void setDimensions(float width, float height) {
        width_ = width;
        height_ = height;
    }

    @Override
    public KernelInstance createKernelInstance(Object context) {
        float[] prev = (float[]) context;
        float[] intersection = GeometryUtil.intersectLineWithBorders(width_, height_, prev[0], prev[1], angle_);
        float distance = -intersection[5] + random_.nextFloat() * (intersection[4] + intersection[5]);
        float destx = (float) (prev[0] + distance * Math.cos(angle_));
        float desty = (float) (prev[1] - distance * Math.sin(angle_));

        Line line = new Line(prev[0], prev[1], destx, desty, 1);
        float[] newContext = new float[]{destx, desty};
        return new KernelInstance(new SingleCurveMultiCurve(line), newContext);
    }
}

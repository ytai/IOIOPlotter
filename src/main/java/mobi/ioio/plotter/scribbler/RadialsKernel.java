package mobi.ioio.plotter.scribbler;

import java.util.Random;

import mobi.ioio.plotter.shapes.Line;
import mobi.ioio.plotter.shapes.SingleCurveMultiCurve;

public class RadialsKernel implements ConstrainedCurveKernel {
    private final Random random_ = new Random();
    private float width_;
    private float height_;
    private final float centerxRel_;
    private final float centeryRel_;
    private float centerx_;
    private float centery_;

    public RadialsKernel(float centerx, float centery) {
        centerxRel_ = centerx;
        centeryRel_ = centery;
    }

    @Override
    public void setDimensions(float width, float height) {
        width_ = width;
        height_ = height;
        centerx_ = width * centerxRel_;
        centery_ = height * centeryRel_;
    }

    @Override
    public KernelInstance createKernelInstance(Object context) {
        float[] prev = (float[]) context;
        float angle = (float) -Math.atan2(prev[1] - centery_, prev[0] - centerx_);
        float[] intersection = GeometryUtil.intersectLineWithBorders(width_, height_, prev[0], prev[1], angle);
        float distance = -intersection[5] + random_.nextFloat() * (intersection[4] + intersection[5]);
        float destx = (float) (prev[0] + distance * Math.cos(angle));
        float desty = (float) (prev[1] - distance * Math.sin(angle));

        Line line = new Line(prev[0], prev[1], destx, desty, 1);
        float[] newContext = new float[]{destx, desty};
        return new KernelInstance(new SingleCurveMultiCurve(line), newContext);
    }
}

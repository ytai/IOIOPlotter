package mobi.ioio.plotter.scribbler;

import java.util.Random;

import mobi.ioio.plotter.Curve;
import mobi.ioio.plotter.shapes.Arc;
import mobi.ioio.plotter.shapes.Line;
import mobi.ioio.plotter.shapes.SingleCurveMultiCurve;

public class ArcsKernel implements ConstrainedCurveKernel {
    private final Random random_ = new Random();
    private float width_;
    private float height_;
    private final float centerxRel_;
    private final float centeryRel_;
    private float centerx_;
    private float centery_;

    public ArcsKernel(float centerx, float centery) {
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
        float startAngle = (float) -Math.atan2(prev[1] - centery_, prev[0] - centerx_);
        float radius = (float) Math.hypot(prev[1] - centery_, prev[0] - centerx_);

        float[] intersection = GeometryUtil.intersectArcWithBorders(width_, height_, centerx_, centery_, radius, startAngle);
        float distance = intersection[1] + random_.nextFloat() * (intersection[0] - intersection[1]);
        float endAngle = startAngle + distance;

        float destx = (float) (centerx_ + radius * Math.cos(endAngle));
        float desty = (float) (centery_ - radius * Math.sin(endAngle));

        Curve arc = new Arc(centerx_, centery_, radius, startAngle, endAngle);

        float[] newContext = new float[]{destx, desty};
        return new KernelInstance(new SingleCurveMultiCurve(arc), newContext);
    }
}

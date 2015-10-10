package mobi.ioio.plotter.scribbler;

import java.util.ArrayList;
import java.util.Random;

import mobi.ioio.plotter.Curve;
import mobi.ioio.plotter.MultiCurve;
import mobi.ioio.plotter.shapes.Circle;
import mobi.ioio.plotter.shapes.ConcatMultiCurve;
import mobi.ioio.plotter.shapes.Line;
import mobi.ioio.plotter.shapes.SingleCurveMultiCurve;

public class CircleKernelFactory implements KernelFactory {
    private Random random_ = new Random();
    private float width_;
    private float height_;
    private float featureSize_;
    private final boolean disjoint_;

    public CircleKernelFactory(boolean disjoint) {
        disjoint_ = disjoint;
    }

    @Override
    public void setDimensions(float width, float height) {
        width_ = width;
        height_ = height;
        featureSize_ = Math.min(width_, height_) / 50;
    }

    @Override
    public KernelInstance createKernelInstance(Object context) {
        float[] startingPoint = (float[]) context;
        if (startingPoint == null) {
            startingPoint = new float[] { random_.nextFloat() * width_,
                                          random_.nextFloat() * height_ };
        }

        float startx = startingPoint[0];
        float starty = startingPoint[1];
        float r = random_.nextFloat() * featureSize_ * 4 + featureSize_;
        float centerx = random_.nextFloat() * (width_ - 2 * r) + r;
        float centery = random_.nextFloat() * (height_ - 2 * r) + r;

        float distToCenter = (float) Math.hypot(startingPoint[0] - centerx,
                                                startingPoint[1] - centery);
        float ratio = (distToCenter + r) / distToCenter;

        float circleStartX = startx + (centerx - startx) * ratio;
        float circleStartY = starty + (centery - starty) * ratio;
        float[] circleStart = {circleStartX, circleStartY};

        Curve circle = Circle.createFromStartAndCenter(centerx, centery, circleStartX, circleStartY);
        Curve line = new Line(startingPoint, circleStart, 1 );

        ArrayList<MultiCurve> list = new ArrayList<MultiCurve>(2);
        list.add(new SingleCurveMultiCurve(line));
        list.add(new SingleCurveMultiCurve(circle));

        return new KernelInstance(new ConcatMultiCurve(list), circleStart);
    }
}

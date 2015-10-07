package mobi.ioio.plotter.scribbler;

import java.util.Random;

import mobi.ioio.plotter.shapes.Line;
import mobi.ioio.plotter.shapes.SingleCurveMultiCurve;

public class LineKernelFactory implements KernelFactory {
    private Random random_ = new Random();
    private float width_;
    private float height_;
    private final boolean disjoint_;

    public LineKernelFactory(boolean disjoint) {
        disjoint_ = disjoint;
    }

    @Override
    public void setDimensions(float width, float height) {
        width_ = width;
        height_ = height;
    }

    @Override
    public KernelInstance createKernelInstance(Object context) {
        float[] from = (float[]) context;
        float[] to;

        if (from == null || disjoint_) {
            from = new float[] { random_.nextFloat() * width_,
                                 random_.nextFloat() * height_ };
        }
        do {
            to = new float[] { random_.nextFloat() * width_,
                               random_.nextFloat() * height_ };
        } while (from.equals(to));

        Line line = new Line(from, to, 1);
        return new KernelInstance(new SingleCurveMultiCurve(line), to);
    }
}

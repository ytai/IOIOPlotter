package mobi.ioio.plotter.scribbler;

import java.util.Random;

import mobi.ioio.plotter.shapes.Line;
import mobi.ioio.plotter.shapes.SingleCurveMultiCurve;

public class CartesianKernelFactory implements KernelFactory {
    private Random random_ = new Random();
    private float width_;
    private float height_;
    private final boolean disjoint_;

    private static class Context {
        public boolean dir;
        public float[] prev;
    }

    public CartesianKernelFactory(boolean disjoint) {
        disjoint_ = disjoint;
    }

    @Override
    public void setDimensions(float width, float height) {
        width_ = width;
        height_ = height;
    }

    @Override
    public KernelInstance createKernelInstance(Object context) {
        Context c = (Context) context;
        float[] to;

        if (c == null) {
            c = new Context();
            c.dir = true;
            c.prev = new float[] { random_.nextFloat() * width_,
                    random_.nextFloat() * height_ };
        } else if (disjoint_) {
            c.prev = new float[] { random_.nextFloat() * width_,
                    random_.nextFloat() * height_ };
        }

        do {
            if (c.dir) {
                to = new float[]{c.prev[0],
                        random_.nextFloat() * height_};
            } else {
                to = new float[]{random_.nextFloat() * width_,
                        c.prev[1]};

            }
        } while (c.prev.equals(to));

        Line line = new Line(c.prev, to, 1);
        Context newContext = new Context();
        newContext.prev = to;
        newContext.dir = !c.dir;
        return new KernelInstance(new SingleCurveMultiCurve(line), newContext);
    }
}

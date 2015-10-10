package mobi.ioio.plotter.scribbler;

import java.util.Random;

import mobi.ioio.plotter.shapes.Line;
import mobi.ioio.plotter.shapes.SingleCurveMultiCurve;

public class ParallelsKernelFactory implements KernelFactory {
    private final float[] dirs_;
    private final Random random_ = new Random();
    private float width_;
    private float height_;

    private static class Context {
        public int dir;
        public float prevx;
        public float prevy;
    }

    public ParallelsKernelFactory(float[] dirs) {
        dirs_ = new float[dirs.length];
        for (int i = 0; i < dirs.length; ++i) {
            dirs_[i] = GeometryUtil.degToRad(dirs[i]);
        }
    }

    @Override
    public void setDimensions(float width, float height) {
        width_ = width;
        height_ = height;
    }

    @Override
    public KernelInstance createKernelInstance(Object context) {
        Context c = (Context) context;

        if (c == null) {
            c = new Context();
            c.dir = random_.nextInt(dirs_.length);
            c.prevx = random_.nextFloat() * width_;
            c.prevy = random_.nextFloat() * height_;
        }

        int newdir = (c.dir + 1 + random_.nextInt(dirs_.length - 1)) % dirs_.length;
        float angle = dirs_[newdir];
        float[] intersection = GeometryUtil.intersectLineWithBorders(width_, height_, c.prevx, c.prevy, angle);
        float distance = -intersection[5] + random_.nextFloat() * (intersection[4] + intersection[5]);
        float destx = (float) (c.prevx + distance * Math.cos(angle));
        float desty = (float) (c.prevy - distance * Math.sin(angle));

        if (destx < 0 || destx > width_ || desty < 0 || desty > height_)
            System.out.printf("WARNING: newdir=%d, angle=%f, prev=(%f, %f), distance=%f, to=(%f, %f)\n",
                    newdir, angle, distance, c.prevx, c.prevy, distance, destx, desty);

        Line line = new Line(c.prevx, c.prevy, destx, desty, 1);
        Context newContext = new Context();
        newContext.prevx = destx;
        newContext.prevy = desty;
        newContext.dir = newdir;
        return new KernelInstance(new SingleCurveMultiCurve(line), newContext);
    }
}

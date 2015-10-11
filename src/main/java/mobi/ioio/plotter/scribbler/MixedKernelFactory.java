package mobi.ioio.plotter.scribbler;

import java.util.Random;

public class MixedKernelFactory implements KernelFactory {
    private final ConstrainedCurveKernel[] kernels_;
    private final Random random_ = new Random();
    private float width_;
    private float height_;

    private static class Context {
        public int lastKernel;
        public float[] lastPos;
    }

    public MixedKernelFactory(ConstrainedCurveKernel[] kernels) {
        kernels_ = kernels;
    }

    @Override
    public void setDimensions(float width, float height) {
        width_ = width;
        height_ = height;
        for (ConstrainedCurveKernel kernel : kernels_) {
            kernel.setDimensions(width, height);
        }
    }

    @Override
    public KernelInstance createKernelInstance(Object context) {
        Context c = (Context) context;

        if (c == null) {
            c = new Context();
            c.lastKernel = random_.nextInt(kernels_.length);
            c.lastPos = new float[] { random_.nextFloat() * width_, random_.nextFloat() * height_ };
        }

        int newKernel = (c.lastKernel + 1 + random_.nextInt(kernels_.length - 1)) % kernels_.length;

        KernelInstance instance = kernels_[newKernel].createKernelInstance(c.lastPos);

        Context newContext = new Context();
        newContext.lastKernel = newKernel;
        newContext.lastPos = (float[]) instance.context_;
        return new KernelInstance(instance.shape_, newContext);
    }
}

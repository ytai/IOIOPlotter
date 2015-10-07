package mobi.ioio.plotter.scribbler;

import mobi.ioio.plotter.MultiCurve;

/**
 * Created by ytai on 10/8/15.
 */
public class KernelInstance {
    public final MultiCurve shape_;
    public final Object context_;


    KernelInstance(MultiCurve shape, Object context) {
        this.shape_ = shape;
        this.context_ = context;
    }
}

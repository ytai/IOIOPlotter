package mobi.ioio.plotter.scribbler;

public interface KernelFactory {
    void setDimensions(float width, float height);
    KernelInstance createKernelInstance(Object context);
}

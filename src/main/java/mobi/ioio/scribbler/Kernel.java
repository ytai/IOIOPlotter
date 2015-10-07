package mobi.ioio.scribbler;

import mobi.ioio.plotter.Curve;

public interface Kernel {
    Curve getCurve();

    Object getContext();
}

package mobi.ioio.plotter;

import java.io.Serializable;

public interface Curve extends Serializable {
    double totalTime();
    void getPosTime(double time, float[] xy);
    float[] getBounds();
}

package mobi.ioio.plotter;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.Iterator;

public class MultiCurveRenderer {
    public static void renderMultiCurve(Mat mat, MultiCurve curve, Scalar color) {
        Iterator<Curve> iter = curve.iterator();
        while (iter.hasNext()) {
            renderCurve(mat, iter.next(), color);
        }
    }

    public static void renderCurve(Mat mat, Curve curve, Scalar color) {
        final double totalTime = curve.totalTime();

        float[] xy = new float[2];
        float[] last = new float[2];
        curve.getPosTime(0, last);

        for (double t = 0; t < totalTime; ) {
            t = Math.min(t + 1, totalTime);
            curve.getPosTime(t, xy);
            Core.line(mat, new Point(last[0], last[1]), new Point(xy[0], xy[1]), color);
            float[] temp = xy;
            xy = last;
            last = temp;
        }
    }
}

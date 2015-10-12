package mobi.ioio.plotter;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class Curve implements Serializable {
    public abstract double totalTime();

    public abstract void getPosTime(double time, float[] xy);

    public abstract float[] getBounds();

    public void renderToMat(Mat mat, Scalar color) {
        final double totalTime = totalTime();

        Point[] points = new Point[(int) Math.ceil(totalTime + 1)];

        float[] xy = new float[2];
        for (int t = 0; t < points.length; ++t) {
            getPosTime(t, xy);
            points[t] = new Point(xy[0], xy[1]);
        }

        MatOfPoint pointsMat = new MatOfPoint();
        pointsMat.fromArray(points);
        List<MatOfPoint> list = new ArrayList<MatOfPoint>(1);
        list.add(pointsMat);

        Core.polylines(mat, list, false, color);
    }
}
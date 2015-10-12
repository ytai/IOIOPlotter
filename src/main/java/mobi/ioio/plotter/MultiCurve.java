package mobi.ioio.plotter;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.io.Serializable;
import java.util.Iterator;

public abstract class MultiCurve implements Serializable {
    private float[] bounds_;
    private boolean hasBounds_ = false;

    public abstract Iterator<Curve> iterator();

    public float[] getBounds() {
        if (!hasBounds_) {
            bounds_ = null;
            Iterator<Curve> iter = iterator();
            while (iter.hasNext()) {
                Curve curve = iter.next();
                float[] curveBounds = curve.getBounds();
                if (curveBounds != null) {
                    if (bounds_ == null) {
                        bounds_ = curveBounds;
                    } else {
                        bounds_[0] = Math.min(bounds_[0], curveBounds[0]);
                        bounds_[1] = Math.min(bounds_[1], curveBounds[1]);
                        bounds_[2] = Math.max(bounds_[2], curveBounds[2]);
                        bounds_[3] = Math.max(bounds_[3], curveBounds[3]);
                    }
                }
            }
            hasBounds_ = true;
        }
        return bounds_ != null ? bounds_.clone() : null;
    }

    public void renderToMat(Mat mat, Scalar color) {
        Iterator<Curve> iter = iterator();
        while (iter.hasNext()) {
            iter.next().renderToMat(mat, color);
        }
    }
}

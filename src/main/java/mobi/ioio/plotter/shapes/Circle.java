package mobi.ioio.plotter.shapes;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import mobi.ioio.plotter.Curve;

public class Circle extends Curve {
	private final float centerX_;
	private final float centerY_;
	private final float radius_;
	final double totalTime_;
	final float radPerSec_;
    final float startAngle_;

    public static Circle createFromStartAndCenter(float centerX, float centerY, float startX, float startY) {
        float radius = (float) Math.hypot(centerY - startY, startX - centerX);
        float startAngle = (float) Math.atan2(centerY - startY, startX - centerX);
        return new Circle(centerX, centerY, radius, startAngle);
    }
	
	public Circle(float centerX, float centerY, float radius, float startAngle) {
		centerX_ = centerX;
		centerY_ = centerY;
		radius_ = radius;
		totalTime_ = 2 * Math.PI * radius;
		radPerSec_ = 1 / radius;
        startAngle_ = startAngle;
	}

    @Override
	public double totalTime() {
		return totalTime_; 
	}

	@Override
	public void getPosTime(double time, float[] xy) {
		final double angle = startAngle_ + limit(time * radPerSec_, 0, (float) (2 * Math.PI));
		xy[0] = (float) (radius_ * Math.cos(angle)) + centerX_;
		xy[1] = (float) (radius_ * Math.sin(angle)) + centerY_;
	}

    @Override
    public float[] getBounds() {
        return new float[] { centerX_ - radius_,
                             centerY_ - radius_,
                             centerX_ + radius_,
                             centerY_ + radius_ };
    }

    @Override
    public void renderToMat(Mat mat, Scalar color) {
        Core.circle(mat, new Point(centerX_, centerY_), Math.round(radius_), color);
    }

    private static double limit(double val, double min, double max) {
		if (val < min) return min;
		if (val > max) return max;
		return val;
	}
}

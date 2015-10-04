package mobi.ioio.plotter.shapes;

import mobi.ioio.plotter.CurvePlotter.Curve;

public class Circle implements Curve {
	private final float centerX_;
	private final float centerY_;
	private final float radius_;
	final double totalTime_;
	final float radPerSec_;
	
	public Circle(float centerX, float centerY, float radius, float mmPerSec) {
		centerX_ = centerX;
		centerY_ = centerY;
		radius_ = radius;
		totalTime_ = 2 * Math.PI * radius / mmPerSec;
		radPerSec_ = mmPerSec / radius;
	}

	@Override
	public double totalTime() {
		return totalTime_; 
	}

	@Override
	public void getPosTime(double time, float[] xy) {
		final double angle = limit(time * radPerSec_, 0, (float) (2 * Math.PI));
		xy[0] = (float) (radius_ * Math.cos(angle)) + centerX_;
		xy[1] = (float) (radius_ * Math.sin(angle)) + centerY_;
	}
	
	private static double limit(double val, double min, double max) {
		if (val < min) return min;
		if (val > max) return max;
		return val;
	}

}

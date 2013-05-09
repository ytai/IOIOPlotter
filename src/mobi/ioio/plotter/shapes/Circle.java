package mobi.ioio.plotter.shapes;

import mobi.ioio.plotter.CurvePlotter.Curve;

public class Circle implements Curve {
	private final float centerX_;
	private final float centerY_;
	private final float radius_;
	final float totalTime_;
	final float radPerSec_;
	
	public Circle(float centerX, float centerY, float radius, float mmPerSec) {
		centerX_ = centerX;
		centerY_ = centerY;
		radius_ = radius;
		totalTime_ = (float) (2 * Math.PI * radius / mmPerSec);
		radPerSec_ = mmPerSec / radius;
	}

	@Override
	public float totalTime() {
		return totalTime_; 
	}

	@Override
	public void getPosTime(float time, float[] xy) {
		final float angle = limit(time * radPerSec_, 0, (float) (2 * Math.PI));
		xy[0] = (float) (radius_ * Math.cos(angle)) + centerX_;
		xy[1] = (float) (radius_ * Math.sin(angle)) + centerY_;
	}
	
	private static float limit(float val, float min, float max) {
		if (val < min) return min;
		if (val > max) return max;
		return val;
	}

}

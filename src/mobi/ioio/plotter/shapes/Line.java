package mobi.ioio.plotter.shapes;

import mobi.ioio.plotter.CurvePlotter.Curve;

public class Line implements Curve {
	private final double totalTime_;
	private final float from_[];
	private final float to_[];

	public Line(float from[], float to[], double mmPerSec) {
		assert from.length == 2;
		assert to.length == 2;

		totalTime_ = Math.hypot(to[0] - from[0], to[1] - from[1]) / mmPerSec;
		from_ = from.clone();
		to_ = to.clone();
	}

	@Override
	public double totalTime() {
		return totalTime_;
	}

	@Override
	public void getPosTime(double time, float[] xy) {
		assert xy.length == 2;

		final double ratio = limit(time / totalTime_, 0, 1);
		xy[0] = (float) (to_[0] * ratio + from_[0] * (1 - ratio));
		xy[1] = (float) (to_[1] * ratio + from_[1] * (1 - ratio));
	}

	private static double limit(double val, double min, double max) {
		if (val < min)
			return min;
		if (val > max)
			return max;
		return val;
	}
}

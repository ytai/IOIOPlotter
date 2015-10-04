package mobi.ioio.plotter;

import ioio.lib.api.Sequencer.ChannelCueBinary;
import ioio.lib.api.Sequencer.ChannelCueSteps;

public class CurvePlotter {
	public static interface Curve {
		public double totalTime();

		public void getPosTime(double time, float[] xy);
	}

	private static final float TICK_RATE = 62500;

	private final StepperController controller_;
	private final CoordinateTransformer transformer_;
	private final int stepTime_;
	private final float[] xy_ = new float[2];
	private final int[] lr_ = new int[2];

	private Curve curve_;
	private long time_;
	private long totalTime_;

	public CurvePlotter(StepperController controller, CoordinateTransformer transformer, float stepTime) {
		controller_ = controller;
		transformer_ = transformer;
		stepTime_ = Math.round(stepTime * TICK_RATE);
	}

	public void setCurve(Curve curve) {
		curve_ = curve;
		time_ = 0;
		totalTime_ = (long) Math.ceil(curve.totalTime() * TICK_RATE);
	}

	public int nextSegment(ChannelCueSteps[] steps, ChannelCueBinary[] dirs) {
		if (time_ >= totalTime_) {
			return 0;
		}
		curve_.getPosTime((time_ + stepTime_) / TICK_RATE, xy_);
		transformer_.xyToLr(xy_, lr_);
		final int time = controller_.goTo(lr_, stepTime_, steps, dirs);
		time_ += time;
		return time;
	}
}

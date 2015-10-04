package mobi.ioio.plotter;

import mobi.ioio.plotter.CurvePlotter.Curve;
import mobi.ioio.plotter.shapes.Delay;
import mobi.ioio.plotter.shapes.Line;
import ioio.lib.api.Sequencer.ChannelCueBinary;
import ioio.lib.api.Sequencer.ChannelCuePwmPosition;
import ioio.lib.api.Sequencer.ChannelCueSteps;

public class Plotter {
	private static final int PEN_UP_PW = 2250;
	private static final int PEN_DOWN_PW = 3850;
	private static final float TICK_RATE = 62500;

	public static interface MultiCurve {
		public Curve nextCurve();

		public float[] getBounds();
	}

	enum State {
		PEN_UP_DELAY, TRANSITION_LINE, PEN_DOWN_DELAY, CURVE
	}

	private static final float STEP_MM = 0.022f;

	private static final float TRANSITION_MM_PER_SEC = 50;

	private static final float[] HOME = { 375, 285 };

	private static final float PEN_DELAY_SEC = 0.5f;

	private final CurvePlotter curvePlotter_;
	private final StepperController controller_ = new StepperController(false, true);
	private final CoordinateTransformer transformer_ = new CoordinateTransformer(754, STEP_MM);
	private final int[] lr_ = new int[2];
	private final float[] xy_ = new float[2];
	private Curve currentCurve_;
	private State state_ = State.CURVE;
	private MultiCurve multiCurve_;

	public void setMultiCurve(MultiCurve multiCurve) {
		multiCurve_ = multiCurve;
		state_ = State.CURVE;
		nextCurve();
	}

	public Plotter(float stepTime) {
		curvePlotter_ = new CurvePlotter(controller_, transformer_, stepTime);
		nextCurve();
	}

	public void getPos(float[] xy) {
		controller_.getCurrentPos(lr_);
		transformer_.lrToXy(lr_, xy);
	}

	public void setPos(float[] xy) {
		transformer_.xyToLr(xy, lr_);
		controller_.setCurrentPos(lr_);
	}

	public int nextSegment(ChannelCueSteps[] steps, ChannelCueBinary[] dirs, ChannelCuePwmPosition servo) {
		servo.pulseWidth = (state_ == State.PEN_UP_DELAY || state_ == State.TRANSITION_LINE) ? PEN_UP_PW : PEN_DOWN_PW;
		int time;
		while ((time = curvePlotter_.nextSegment(steps, dirs)) == 0) {
			if (!nextCurve()) {
				// We're done!
				return 0;
			}
		}
		return time;
	}

	public int manualDelta(float[] delta, float time, ChannelCueSteps[] steps, ChannelCueBinary[] dirs) {
		lr_[0] = Math.round(delta[0] / STEP_MM);
		lr_[1] = Math.round(delta[1] / STEP_MM);
		return controller_.goToDelta(lr_, Math.round(time * TICK_RATE), steps, dirs);
	}

	public int manualPenUp(ChannelCueSteps[] steps, ChannelCueBinary[] dirs, ChannelCuePwmPosition servo) {
		servo.pulseWidth = PEN_UP_PW;
		return manualDelta(new float[2], PEN_DELAY_SEC, steps, dirs);
	}

	public int manualPenDown(ChannelCueSteps[] steps, ChannelCueBinary[] dirs, ChannelCuePwmPosition servo) {
		servo.pulseWidth = PEN_DOWN_PW;
		return manualDelta(new float[2], PEN_DELAY_SEC, steps, dirs);
	}

	public void setXy(float x, float y) {
		xy_[0] = x;
		xy_[1] = y;
		transformer_.xyToLr(xy_, lr_);
		controller_.setCurrentPos(lr_);
	}

	private boolean nextCurve() {
		final float[] currentPos = new float[2];
		getPos(currentPos);

		switch (state_) {
		case PEN_UP_DELAY:
			// Pen is up.
			// Generate a line curve from our current position to the beginning
			// of the next curve, or back HOME if we're done.
			final float[] to;
			if (currentCurve_ != null) {
				to = new float[2];
				currentCurve_.getPosTime(0, to);
			} else {
				to = HOME;
			}

			// Go!
			curvePlotter_.setCurve(new Line(currentPos, to, TRANSITION_MM_PER_SEC));
			state_ = State.TRANSITION_LINE;
			break;

		case TRANSITION_LINE:
			// Just finished the transition. Wait for the pen to get down.
			if (currentCurve_ != null) {
				curvePlotter_.setCurve(new Delay(currentPos, PEN_DELAY_SEC));
			} else {
				// Otherwise, we're done!
				return false;
			}
			state_ = State.PEN_DOWN_DELAY;
			break;

		case PEN_DOWN_DELAY:
			// Pen is down, we can start plotting!
			curvePlotter_.setCurve(currentCurve_);
			state_ = State.CURVE;
			break;

		case CURVE:
			// Just finished a curve, raise the pen.
			currentCurve_ = multiCurve_ != null ? multiCurve_.nextCurve() : null;
			curvePlotter_.setCurve(new Delay(currentPos, PEN_DELAY_SEC));
			state_ = State.PEN_UP_DELAY;
			break;
		}
		return true;
	}

	public void setLr(float l, float r) {
		int[] pos = { Math.round(l / STEP_MM), Math.round(r / STEP_MM) };
		controller_.setCurrentPos(pos);
	}
}

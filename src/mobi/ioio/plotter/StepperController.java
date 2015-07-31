package mobi.ioio.plotter;

import ioio.lib.api.Sequencer.ChannelCueBinary;
import ioio.lib.api.Sequencer.ChannelCueSteps;
import ioio.lib.api.Sequencer.Clock;
import ioio.lib.spi.Log;

public class StepperController {
	private static final String TAG = "StepperController";
	private static final int PULSE_WIDTH = 1; // In 16[us] units.
	private final int numChannels_;
	private final boolean[] positiveDirs_;
	private final int[] currentPos_;
	private final int[] delta_;
	private final boolean[] signs_;

	enum Result {
		OK, TOO_FAST, TOO_LONG
	}

	public StepperController(boolean... positiveDirs) {
		positiveDirs_ = positiveDirs.clone();
		numChannels_ = positiveDirs.length;
		delta_ = new int[numChannels_];
		signs_ = new boolean[numChannels_];
		currentPos_ = new int[numChannels_];
	}

	public void setCurrentPos(int[] pos) {
		System.arraycopy(pos, 0, currentPos_, 0, pos.length);
	}

	public void getCurrentPos(int[] pos) {
		pos[0] = currentPos_[0];
		pos[1] = currentPos_[1];
	}

	public int goToDelta(int[] delta, int time, ChannelCueSteps[] steps, ChannelCueBinary[] dirs) {
		assert delta.length == numChannels_;
		assert steps.length == numChannels_;
		assert dirs.length == numChannels_;

		if (delta != delta_) {
			System.arraycopy(delta, 0, delta_, 0, delta.length);
		}

		sign(signs_, delta_);
		abs(delta_);

		while (time > 0) {
			Result r = attemptDelta(delta_, time, steps);

			if (r == Result.OK) {
				break;
			}
			divide(delta_, 2);
			if (r == Result.TOO_LONG) {
				Log.v(TAG, "Segment too long.");
				time /= 2;
			} else {
				Log.v(TAG, "Segment too fast.");
			}
		}
		setDirections(dirs);
		sign(delta_, signs_);
		plus(currentPos_, delta_);

		if (delta != delta_) {
			minus(delta, delta_);
		}

		return time;
	}

	public int goTo(int[] pos, int time, ChannelCueSteps[] steps, ChannelCueBinary[] dirs) {
		assert pos.length == numChannels_;
		assert steps.length == numChannels_;
		assert dirs.length == numChannels_;

		System.arraycopy(pos, 0, delta_, 0, pos.length);
		minus(delta_, currentPos_);
		return goToDelta(delta_, time, steps, dirs);
	}

	private void setDirections(ChannelCueBinary[] dirs) {
		assert dirs.length == numChannels_;

		for (int index = 0; index < numChannels_; ++index) {
			dirs[index].value = !(signs_[index] ^ positiveDirs_[index]);
		}
	}

	private static void sign(int[] vec, boolean[] keep) {
		assert keep.length == vec.length;
		for (int index = 0; index < vec.length; ++index) {
			if (!keep[index]) {
				vec[index] = -vec[index];
			}
		}
	}

	private static void abs(int[] vec) {
		for (int index = 0; index < vec.length; ++index) {
			vec[index] = Math.abs(vec[index]);
		}
	}

	private static void sign(boolean[] sgn, int[] vec) {
		assert sgn.length == vec.length;
		for (int index = 0; index < vec.length; ++index) {
			sgn[index] = vec[index] >= 0;
		}
	}

	private static void minus(int[] a, int[] b) {
		assert a.length == b.length;
		for (int index = 0; index < a.length; ++index) {
			a[index] -= b[index];
		}
	}

	private static void plus(int[] a, int[] b) {
		assert a.length == b.length;
		for (int index = 0; index < a.length; ++index) {
			a[index] += b[index];
		}
	}

	private static void divide(int[] pos, float div) {
		for (int index = 0; index < pos.length; ++index) {
			pos[index] /= div;
		}
	}

	private Result attemptDelta(int[] delta, int time, ChannelCueSteps[] steps) {
		for (int i = 0; i < numChannels_; ++i) {
			Result r = attemptDeltaChannel(delta[i], time, steps[i]);
			if (r != Result.OK)
				return r;
		}
		return Result.OK;
	}

	private static Result attemptDeltaChannel(int delta, int time, ChannelCueSteps cue) {
		assert delta >= 0;

		if (time > (1 << 16)) {
			return Result.TOO_LONG;
		}

		if (delta == 0) {
			cue.clk = Clock.CLK_16M;
			cue.pulseWidth = 0;
			return Result.OK;
		}

		int period = time * 256 / delta; // in 1/16[us] units.
		int pulseWidth = PULSE_WIDTH * 256;
		int minGap;
		if (period > (1 << 16) * 256) {
			// Too long.
			return Result.TOO_LONG;
		} else if (period > (1 << 16) * 64) {
			// Need a 256x prescaler.
			cue.clk = Clock.CLK_62K5;
			minGap = 1;
			pulseWidth /= 256;
			period /= 256;
		} else if (period > (1 << 16) * 8) {
			// Need a 64x prescaler.
			cue.clk = Clock.CLK_250K;
			minGap = 2;
			pulseWidth /= 64;
			period /= 64;
			time *= 4;
		} else if (period > (1 << 16)) {
			// Need a 8x prescaler.
			cue.clk = Clock.CLK_2M;
			minGap = 13;
			pulseWidth /= 8;
			period /= 8;
			time *= 32;
		} else if (period < 3) {
			return Result.TOO_FAST;
		} else {
			// No need for a prescaler.
			cue.clk = Clock.CLK_16M;
			minGap = 96;
			time *= 256;
		}

		// From this point and on, all times are in the channelCueSteps.clk
		// units.

		// Now we need to verify that:
		// 1. We'll actually have delta steps in this segment. We might have
		// more due to rounding errors.
		if (time / period != delta) {
			return Result.TOO_LONG;
		}

		// 2. We have enough gap between the end of the last pulse and the end
		// of the segment.
		final int endOfLastPulse = (delta - 1) * period + (pulseWidth + period - 1) / 2;
		if (time - endOfLastPulse < minGap) {
			return Result.TOO_LONG;
		}
		// Success!
		cue.period = period;
		cue.pulseWidth = pulseWidth;
		return Result.OK;
	}
}

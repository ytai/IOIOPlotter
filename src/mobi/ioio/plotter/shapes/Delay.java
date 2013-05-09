package mobi.ioio.plotter.shapes;

import mobi.ioio.plotter.CurvePlotter.Curve;

public class Delay implements Curve {
	private final float pos_[];
	private final float time_;
	
	
	public Delay(float pos[], float time) {
		pos_ = pos.clone();
		time_ = time;
	}

	@Override
	public float totalTime() {
		return time_;
	}

	@Override
	public void getPosTime(float time, float[] xy) {
		xy[0] = pos_[0];
		xy[1] = pos_[1];
	}
}

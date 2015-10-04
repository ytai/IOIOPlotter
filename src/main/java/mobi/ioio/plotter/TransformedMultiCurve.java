package mobi.ioio.plotter;

import mobi.ioio.plotter.CurvePlotter.Curve;
import mobi.ioio.plotter.Plotter.MultiCurve;

public class TransformedMultiCurve implements MultiCurve {
	final float[] offset_ = new float[2];
	final float scale_;
	final float timeScale_;
	final MultiCurve mutliCurve_;

	public TransformedMultiCurve(MultiCurve mutliCurve, float[] offset, float scale, float timeScale) {
		mutliCurve_ = mutliCurve;
		System.arraycopy(offset, 0, offset_, 0, offset_.length);
		scale_ = scale;
		timeScale_ = timeScale;
	}

	@Override
	public Curve nextCurve() {
		Curve curve = mutliCurve_.nextCurve();
		if (curve == null)
			return null;
		return new TransformedCurve(curve, offset_, scale_, timeScale_);
	}

	@Override
	public float[] getBounds() {
		float[] bounds = mutliCurve_.getBounds();
		bounds[0] *= scale_;
		bounds[1] *= scale_;
		return bounds;
	}

}

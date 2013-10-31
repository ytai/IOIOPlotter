package mobi.ioio.plotter.shapes;

import java.io.Serializable;

import mobi.ioio.plotter.CurvePlotter.Curve;
import mobi.ioio.plotter.Plotter.MultiCurve;

public class SingleCurveMultiCurve implements MultiCurve, Serializable {
	private static final long serialVersionUID = -8271726700104333260L;
	private final float[] bounds_;
	private Curve curve_;

	public SingleCurveMultiCurve(Curve curve, float[] bounds) {
		bounds_ = bounds;
		curve_ = curve;
	}

	@Override
	public Curve nextCurve() {
		Curve curve = curve_;
		curve_ = null;
		return curve;
	}

	@Override
	public float[] getBounds() {
		return bounds_;
	}
}
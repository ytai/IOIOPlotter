package mobi.ioio.plotter.shapes;

import java.util.ArrayList;
import java.util.List;

import mobi.ioio.plotter.CurvePlotter.Curve;
import mobi.ioio.plotter.Plotter.MultiCurve;

public class Target implements MultiCurve {
	List<Curve> curves_ = new ArrayList<Curve>(6);
	float[] bounds_;

	public Target(float x, float y, float r, float mmPerSec) {
		curves_.add(new Circle(x, y, r, mmPerSec));
		curves_.add(new Circle(x, y, r / 2, mmPerSec));
		curves_.add(new Line(new float[] { x, y - r }, new float[] { x, y + r }, mmPerSec));
		curves_.add(new Line(new float[] { x - r, y }, new float[] { x + r, y }, mmPerSec));
		curves_.add(new Line(new float[] { x - r, y - r }, new float[] { x + r, y + r }, mmPerSec));
		curves_.add(new Line(new float[] { x - r, y + r }, new float[] { x + r, y - r }, mmPerSec));
		bounds_ = new float[] { x - r, y - r, x + r, y + r };
	}

	@Override
	public Curve nextCurve() {
		if (curves_.isEmpty())
			return null;
		return curves_.remove(0);
	}

	@Override
	public float[] getBounds() {
		return bounds_.clone();
	}

}

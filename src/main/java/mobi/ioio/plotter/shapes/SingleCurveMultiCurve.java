package mobi.ioio.plotter.shapes;

import java.io.Serializable;
import java.util.Iterator;

import mobi.ioio.plotter.Curve;
import mobi.ioio.plotter.MultiCurve;

public class SingleCurveMultiCurve extends MultiCurve implements Serializable {
	private static final long serialVersionUID = -8271726700104333260L;
	private Curve curve_;

	public SingleCurveMultiCurve(Curve curve) {
		curve_ = curve;
	}

   @Override
    public Iterator<Curve> iterator() {
        return new Iterator<Curve>() {
            Curve current_ = curve_;

            @Override
            public boolean hasNext() {
                return current_ != null;
            }

            @Override
            public Curve next() {
                Curve result = current_;
                current_ = null;
                return result;
            }

            @Override
            public void remove() {
                throw new RuntimeException("Not supported.");
            }
        };
    }

    @Override
	public float[] getBounds() {
        return curve_.getBounds();
	}

    @Override
    public double totalTime() {
        return curve_.totalTime();
    }
}
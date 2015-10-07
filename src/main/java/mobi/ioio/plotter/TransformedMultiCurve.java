package mobi.ioio.plotter;

import java.util.Iterator;

public class TransformedMultiCurve extends MultiCurve {
	final float[] offset_ = new float[2];
	final float scale_;
	final float timeScale_;
	final MultiCurve mutliCurve_;

    private class Iter implements Iterator<Curve> {
        private final Iterator<Curve> underlying_;

        Iter() {
            underlying_ = mutliCurve_.iterator();
        }

        @Override
        public boolean hasNext() {
            return underlying_.hasNext();
        }

        @Override
        public Curve next() {
            return new TransformedCurve(underlying_.next(), offset_, scale_, timeScale_);
        }

        @Override
        public void remove() {
            throw new RuntimeException("Not implemented.");
        }
    }

	public TransformedMultiCurve(MultiCurve mutliCurve, float[] offset, float scale, float timeScale) {
		mutliCurve_ = mutliCurve;
		System.arraycopy(offset, 0, offset_, 0, offset_.length);
		scale_ = scale;
		timeScale_ = timeScale;
	}

    @Override
    public Iterator<Curve> iterator() {
        return new Iter();
    }
}

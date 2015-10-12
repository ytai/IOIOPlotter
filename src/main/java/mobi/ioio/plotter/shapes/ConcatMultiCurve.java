package mobi.ioio.plotter.shapes;

import java.util.Collection;
import java.util.Iterator;

import mobi.ioio.plotter.Curve;
import mobi.ioio.plotter.MultiCurve;

public class ConcatMultiCurve extends MultiCurve {
    private final Collection<MultiCurve> curves_;
    private double totalTime_ = Double.NaN;

    private class Iter implements Iterator<Curve> {
        Iterator<MultiCurve> parent_ = curves_.iterator();
        Iterator<Curve> child_;

        Iter() {
            advanceParent();
        }

        private void advanceParent() {
            while (parent_.hasNext()) {
                child_ = parent_.next().iterator();
                if (child_.hasNext()) return;
            }
            child_ = null;
        }

        @Override
        public boolean hasNext() {
            return child_ != null;
        }

        @Override
        public Curve next() {
            Curve result = child_.next();
            if (!child_.hasNext()) {
                advanceParent();
            }
            return result;
        }

        @Override
        public void remove() {
            throw new RuntimeException("Not implemented.");
        }
    }

    public ConcatMultiCurve(Collection<MultiCurve> curves) {
        curves_ = curves;
    }

    @Override
    public Iterator<Curve> iterator() {
        return new Iter();
    }

    @Override
    public double totalTime() {
        if (Double.isNaN(totalTime_)) {
            totalTime_ = 0;
            for (MultiCurve c : curves_) {
                totalTime_ += c.totalTime();
            }
        }
        return totalTime_;
    }
}

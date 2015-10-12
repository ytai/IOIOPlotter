package mobi.ioio.plotter.shapes;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import mobi.ioio.plotter.Curve;
import mobi.ioio.plotter.MultiCurve;
import mobi.ioio.plotter.trace.BinaryImage;
import mobi.ioio.plotter.trace.BinaryImageTracer;


public class BinaryImageMultiCurve extends MultiCurve implements Serializable {
	private static final long serialVersionUID = -3015846915211337975L;
    private final BinaryImage image_;
	private final int minCurvePixels_;
    private double totalTime_ = Double.NaN;

    private class Iter implements Iterator<Curve> {
        private final BinaryImageTracer tracer_;
        private Curve next_;

        private Iter() {
            tracer_ = new BinaryImageTracer(image_);
            next_ = nextCurve();
        }

        @Override
        public boolean hasNext() {
            return next_ != null;
        }

        @Override
        public Curve next() {
            Curve result = next_;
            next_ = nextCurve();
            return result;
        }

        @Override
        public void remove() {
            throw new RuntimeException("Not implemented.");
        }

        private Curve nextCurve() {
            while (tracer_.nextCurve()) {
                List<int[]> chain = new LinkedList<int[]>();
                boolean hasMore = true;
                while (hasMore) {
                    int[] xy = new int[2];
                    hasMore = tracer_.nextSegment(xy);
                    chain.add(xy);
                }
                if (chain.size() >= minCurvePixels_) {
                    return new TraceCurve(chain);
                }
            }
            return null;
        }
        private class TraceCurve extends Curve {
            private final int[][] chain_;
            private final double[] times_;
            private int currentIndex_ = 0;
            private float[] bounds_ = null;

            public TraceCurve(List<int[]> chain) {
                chain_ = new int[chain.size()][2];
                int j = 0;
                for (int[] xy : chain) {
                    chain_[j][0] = xy[0];
                    chain_[j][1] = xy[1];
                    j++;

                    if (bounds_ == null) {
                        bounds_ = new float[] { xy[0], xy[1], xy[0], xy[1] };
                    } else {
                        bounds_[0] = Math.min(bounds_[0], xy[0]);
                        bounds_[1] = Math.min(bounds_[1], xy[1]);
                        bounds_[2] = Math.max(bounds_[2], xy[0]);
                        bounds_[3] = Math.max(bounds_[3], xy[1]);
                    }
                }
                times_ = new double[chain_.length];
                double time = 0;
                for (int i = 0; i < chain_.length; ++i) {
                    times_[i] = time;
                    if (i < chain_.length - 1) {
                        final float distToNext = (float) Math.hypot(chain_[i + 1][0] - chain_[i][0], chain_[i + 1][1]
                                - chain_[i][1]);
                        time += distToNext;
                    }
                }
            }

            @Override
            public double totalTime() {
                return times_[times_.length - 1];
            }

            @Override
            public void getPosTime(double time, float[] xy) {
                assert time >= times_[currentIndex_];
                while (currentIndex_ < times_.length - 1 && times_[currentIndex_ + 1] <= time) {
                    ++currentIndex_;
                }
                if (currentIndex_ == times_.length - 1) {
                    // Last point.
                    xy[0] = chain_[currentIndex_][0];
                    xy[1] = chain_[currentIndex_][1];
                } else {
                    // Linear interpolation.
                    final double ratio = (time - times_[currentIndex_]) / (times_[currentIndex_ + 1] - times_[currentIndex_]);
                    xy[0] = (float) ((1 - ratio) * chain_[currentIndex_][0] + ratio * chain_[currentIndex_ + 1][0]);
                    xy[1] = (float) ((1 - ratio) * chain_[currentIndex_][1] + ratio * chain_[currentIndex_ + 1][1]);
                }
            }

            @Override
            public float[] getBounds() {
                return bounds_.clone();
            }
        }
    }

	public BinaryImageMultiCurve(BinaryImage image, int minCurvePixels) {
        image_ = image;
		minCurvePixels_ = minCurvePixels;
	}

    @Override
    public Iterator<Curve> iterator() {
        return new Iter();
    }

    @Override
    public double totalTime() {
        if (Double.isNaN(totalTime_)) {
            totalTime_ = 0;
            Iterator<Curve> iter = iterator();
            while (iter.hasNext()) {
                totalTime_ += iter.next().totalTime();
            }
        }
        return totalTime_;
    }
}

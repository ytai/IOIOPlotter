package mobi.ioio.plotter.shapes;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import mobi.ioio.plotter.CurvePlotter.Curve;
import mobi.ioio.plotter.Plotter.MultiCurve;
import mobi.ioio.plotter.trace.BinaryImage;
import mobi.ioio.plotter.trace.BinaryImageTracer;


public class BinaryImageMultiCurve implements MultiCurve, Serializable {
	private static final long serialVersionUID = -3015846915211337975L;
	private final BinaryImageTracer tracer_;
	private final int minCurvePixels_;
	private final float[] bounds_;

	public BinaryImageMultiCurve(BinaryImage image, int minCurvePixels) {
		tracer_ = new BinaryImageTracer(image);
		minCurvePixels_ = minCurvePixels;
		bounds_ = new float[] { 0, 0, image.width_, image.height_ };
	}

	@Override
	public Curve nextCurve() {
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
	
	private class TraceCurve implements Curve {
		private final int[][] chain_;
		private final double[] times_;
		private int currentIndex_ = 0;

		public TraceCurve(List<int[]> chain) {
			chain_ = new int[chain.size()][2];
			int j = 0;
			for (int[] xy : chain) {
				chain_[j][0] = xy[0];
				chain_[j][1] = xy[1];
				j++;
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
	}

	@Override
	public float[] getBounds() {
		return bounds_.clone();
	}
}

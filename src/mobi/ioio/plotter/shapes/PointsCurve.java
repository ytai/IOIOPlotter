package mobi.ioio.plotter.shapes;

import java.io.IOException;
import java.io.Serializable;

import mobi.ioio.plotter.CurvePlotter.Curve;

import org.opencv.core.Point;

public class PointsCurve implements Curve, Serializable {
	private static final long serialVersionUID = -7137155928229534506L;
	private float totalLength_;
	private Point[] points_;
	int currentPointIndex_ = 0;
	float timeOfCurrentPoint_ = 0;

	public PointsCurve(Point[] points) {
		assert points.length > 1;
		totalLength_ = totalLength(points);
		points_ = points;
	}

	@Override
	public float totalTime() {
		return totalLength_;
	}

	@Override
	public void getPosTime(float time, float[] xy) {
		if (time < 0) {
			xy[0] = (float) points_[0].x;
			xy[1] = (float) points_[0].y;
			return;
		}
		float timeFromCurrent = time - timeOfCurrentPoint_;
		while (true) {
			if (currentPointIndex_ == points_.length - 1) {
				xy[0] = (float) points_[currentPointIndex_].x;
				xy[1] = (float) points_[currentPointIndex_].y;
				return;
			}
			float timeToNext = dist(points_[currentPointIndex_], points_[currentPointIndex_ + 1]);
			if (timeToNext > timeFromCurrent) {
				final float x0 = (float) points_[currentPointIndex_].x;
				final float y0 = (float) points_[currentPointIndex_].y;
				final float dx = (float) points_[currentPointIndex_ + 1].x - x0;
				final float dy = (float) points_[currentPointIndex_ + 1].y - y0;
				final float ratio = timeFromCurrent / timeToNext;
				xy[0] = x0 + dx * ratio;
				xy[0] = y0 + dy * ratio;
				return;
			}
			timeFromCurrent -= timeToNext;
			timeOfCurrentPoint_ += timeToNext;
			++currentPointIndex_;
		}
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeInt(points_.length);
		for (Point p : points_) {
			out.writeDouble(p.x);
			out.writeDouble(p.y);
		}
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException {
		points_ = new Point[in.readInt()];
		for (int i = 0; i < points_.length; ++i) {
			points_[i] = new Point(in.readDouble(), in.readDouble());			
		}
		totalLength_ = totalLength(points_);
	}

	static float totalLength(Point[] points) {
		if (points.length < 2) {
			return 0;
		}
		float length = 0;
		for (int i = 1; i < points.length; ++i) {
			length += dist(points[i], points[i - 1]);
		}
		return length;
	}

	static float dist(Point a, Point b) {
		return (float) Math.hypot(a.x - b.x, a.y - b.y);
	}
}
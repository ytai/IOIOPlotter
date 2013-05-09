package mobi.ioio.plotter;

public class CoordinateTransformer {
	private final float dist_;
	private final float step_;
	private final float sqStep_;
	
	public CoordinateTransformer(float dist, float step) {
		dist_ = dist;
		step_ = step;
		sqStep_ = step * step;
	}
	
	public void xyToLr(float[] xy, int[] lr) {
		assert xy.length == 2;
		assert lr.length == 2;
		
		final float dx = dist_ - xy[0];
		
		lr[0] = (int) Math.round(Math.sqrt(xy[0]*xy[0] + xy[1]*xy[1]) / step_);
		lr[1] = (int) Math.round(Math.sqrt(dx * dx + xy[1]*xy[1]) / step_);
	}
	
	public void lrToXy(int[] lr, float[] xy) {
		xy[0] = (dist_ * dist_ + sqStep_ * (lr[0] * lr[0] - lr[1] * lr[1])) / (2 * dist_);
		xy[1] = (float) Math.sqrt(sqStep_ * (lr[0] * lr[0]) - xy[0] * xy[0]);
	}
}

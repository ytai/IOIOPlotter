package mobi.ioio.plotter;

public class TransformedCurve implements Curve {
	final float[] offset_ = new float[2];
	final float scale_;
	final float timeScale_;
	final Curve curve_;

	public TransformedCurve(Curve curve, float[] offset, float scale, float timeScale) {
		curve_ = curve;
		System.arraycopy(offset, 0, offset_, 0, offset_.length);
		scale_ = scale;
		timeScale_ = timeScale * scale;
	}
	
	@Override
	public double totalTime() {
		return curve_.totalTime() * timeScale_;
	}

	@Override
	public void getPosTime(double time, float[] xy) {
		curve_.getPosTime(time / timeScale_, xy);
		transform(xy);
	}

    @Override
    public float[] getBounds() {
        float[] bounds = curve_.getBounds();
        transform(bounds);
        return bounds;
    }

    private void transform(float[] xy) {
		for (int i = 0; i < xy.length; ++i) {
			xy[i] = xy[i] * scale_ + offset_[i];
		}
	}
}

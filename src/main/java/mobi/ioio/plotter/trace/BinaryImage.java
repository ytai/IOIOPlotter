package mobi.ioio.plotter.trace;
import java.io.Serializable;
import java.util.BitSet;

public class BinaryImage implements Serializable {
	private static final long serialVersionUID = -1842891593687813523L;

	private static final int DIRS[][] = {{3, 4, 5}, {2, -1, 6}, {1, 0, 7}};
	
	private final BitSet bits_;
	public final int width_;
	public final int height_;
	public final int numPixels_;

	public BinaryImage(BitSet bits, int width) {
		assert bits.length() % width == 0;

		bits_ = bits;
		width_ = width;
		height_ = bits.length() / width;
		numPixels_ = bits.length();
	}

	public BinaryImage(int width, int height) {
		width_ = width;
		height_ = height;
		numPixels_ = height * width;
		bits_ = new BitSet(numPixels_);
	}
	
	@Override
	public BinaryImage clone() {
		return new BinaryImage((BitSet) bits_.clone(), width_);
	}

	public void set(int x, int y, boolean value) {
		set(index(x, y), value);
	}

	public boolean get(int x, int y) {
		return get(index(x, y));
	}

	public boolean get(int index) {
		return bits_.get(index);
	}

	public void set(int index, boolean value) {
		bits_.set(index, value);
	}

	public int index(int x, int y) {
		return y * width_ + x;
	}

	public void coordinates(int index, int[] xy) {
		assert xy.length == 2;
		xy[0] = index % width_;
		xy[1] = index / width_;
	}
	
	public byte env8(int[] xy) {
		byte result = 0;
		if (xy[0] > 0 && xy[0] < width_ - 1 && xy[1] > 0 && xy[1] < height_ - 1) {
			// Common, fast case.
			int index = index(xy[0], xy[1]);
			for (int dir = 0; dir < 8; ++dir) {
				if (get(move(index, dir))) {
					result |= (1 << dir);
				}
			}
		} else {
			for (int dy = -1; dy <= 1; ++dy) {
				for (int dx = -1; dx <= 1; ++dx) {
					final int x = xy[0] + dx;
					final int y = xy[1] + dy;
					if (dx == 0 && dy == 0) continue;
					if (x >= 0 && x < width_ && y >= 0 && y < height_ && get(x, y)) {
						result |= (1 << dir(dx, dy));
					}
				}
			}
		}
		return result;
	}

	public int move(int index, int dir) {
		switch (dir) {
		case 0:
			return index + 1;
		case 1:
			return index + 1 - width_;
		case 2:
			return index - width_;
		case 3:
			return index - 1 - width_;
		case 4:
			return index - 1;
		case 5:
			return index - 1 + width_;
		case 6:
			return index + width_;
		case 7:
			return index + width_ + 1;
		default:
			return -1;
		}
	}
	
	private int dir(int dx, int dy) {
		assert dx != 0 || dy != 0;
		return DIRS[dx + 1][dy + 1];
	}
}

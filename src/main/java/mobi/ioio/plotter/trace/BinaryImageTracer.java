package mobi.ioio.plotter.trace;

import java.io.Serializable;

public class BinaryImageTracer implements Serializable {
	private static final long serialVersionUID = -5180052279734619854L;
	private final BinaryImage image_;
	private final BinaryImage origImage_;
	private int segmentIndex_ = 0;
	private int scanIndex_ = 0;
	private int dir_;
//	private final int tmpxy[] = new int[2];
//	private boolean zigzagDir_ = true;
	
	private static final int[] TURNS = { 0, 1, -1, 2, -2, 3, -3 }; 

	public BinaryImageTracer(BinaryImage image) {
		origImage_ = image;
		image_ = image.clone();
	}

	public boolean nextCurve() {
		while (scanIndex_ < image_.numPixels_) {
			if (image_.get(scanIndex_)) {
				dir_ = 0;
				segmentIndex_ = scanIndex_;
				return true;
			}
			++scanIndex_;
		}
		return false;
	}
	
//	private void advanceScanIndex() {
//		image_.coordinates(scanIndex_, tmpxy);
//		int dir;
//		if (zigzagDir_) {
//			// Try to move up and to the right.
//			if (tmpxy[0] == image_.width_ - 1) {
//				// Reached the right edge. Go down.
//				dir = 6;
//				zigzagDir_ = false;
//			} else if (tmpxy[1] == 0) {
//				// Reached the top. Go right.
//				dir = 0;
//				zigzagDir_ = false;
//			} else {
//				dir = 1;
//			}
//		} else {
//			// Try to move down and to the left.
//			if (tmpxy[1] == image_.height_ - 1) {
//				// Reached the bottom. Go right.
//				dir = 0;
//				zigzagDir_ = true;
//			} else if (tmpxy[0] == 0) {
//				// Reached the left edge. Go down.
//				dir = 6;
//				zigzagDir_ = true;
//			} else {
//				dir = 5;
//			}
//		}
//		scanIndex_ = image_.move(scanIndex_, dir);
//	}

	public boolean nextSegment(int[] xy) {
		image_.coordinates(segmentIndex_, xy);

		// This will happen if we just completed the current curve by connecting to an existing curve.
		if (!image_.get(segmentIndex_)) return false;
		
		image_.set(segmentIndex_, false);

		// Look if we have a next point to go to.
		byte env8 =  image_.env8(xy);
		for (int i = 0; i < TURNS.length; ++i) {
			final int newdir = (dir_ + TURNS[i] + 8) % 8;
			if ((env8 & (1 << newdir)) != 0) {
				move(newdir);
				return true;
			}
		}
		
		// Otherwise, look whether we can connect to an existing curve, but only if it is not behind.
		env8 =  origImage_.env8(xy);
		for (int i = 0; i < 5; ++i) {
			final int newdir = (dir_ + TURNS[i] + 8) % 8;
			if ((env8 & (1 << newdir)) != 0) {
				move(newdir);
				return false;
			}
		}

		return false;
	}
	
	private void move(int dir) {
		dir_ = dir;
		segmentIndex_ = image_.move(segmentIndex_, dir);
	}
}

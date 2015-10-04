package mobi.ioio.plotter_app;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.net.Uri;

public class Scribbler implements Runnable {
	enum Mode {
		Raster, Vector
	}

	interface Listener {
		public void previewFrame(Bitmap frame);

		public void progress(float darkness, int numLines);

		public void result(Point[] points, final Rect bounds, Bitmap thumbnail);
	}

	private final Thread thread_;
	private final Context context_;
	private final Uri uri_;
	private float blur_;
	private float threshold_;
	private Mode mode_;
	private Listener listener_;
	private boolean requestResult_ = false;

	// Internals
	private static final float LINE_WIDTH_TO_IMAGE_WIDTH = 450;
	private static final float GRAY_RESOLUTION = 128;
	private static final int NUM_ATTEMPTS = 100;
	private static final int MAX_LINES = 2000;
	private static Random random_ = new Random();
	private Mat srcImage_;
	private Mat imageScaledToPreview_;
	private Mat imageResidue_;
	private Mat previewImage_;
	private float currentBlur_;
	private float currentThreshold_;
	private Mode currentMode_;
	private SortedMap<Float, Point> lines_ = new TreeMap<Float, Point>();
	private Bitmap previewBitmap_;
	boolean stopped_ = false;

	public Scribbler(Context context, Uri uri, float initialBlur, float initialThershold,
			Mode initialMode, Listener listener) {
		context_ = context;
		uri_ = uri;
		blur_ = initialBlur;
		threshold_ = initialThershold;
		mode_ = initialMode;
		listener_ = listener;

		thread_ = new Thread(this);
		thread_.start();
	}

	public synchronized void stop() {
		stopped_ = true;
		listener_ = null;
		thread_.interrupt();
	}

	public synchronized void setBlur(float blur) {
		blur_ = blur;
		notify();
	}

	public synchronized void setThreshold(float threshold) {
		threshold_ = threshold;
		notify();
	}

	public synchronized void setMode(Mode mode) {
		mode_ = mode;
		notify();
	}

	public synchronized void requestResult() {
		requestResult_ = true;
		notify();
	}

	@Override
	public void run() {
		try {
			load();
			while (!stopped_) {
				try {
					step();
				} catch (InterruptedException e) {
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void load() throws MalformedURLException, IOException {
		InputStream stream = resolveUri(context_, uri_);
		Mat buf = streamToMat(stream);
		srcImage_ = Highgui.imdecode(buf, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
		float scale = LINE_WIDTH_TO_IMAGE_WIDTH / srcImage_.cols();
		imageScaledToPreview_ = new Mat();
		imageResidue_ = new Mat();
		Imgproc.resize(srcImage_, imageScaledToPreview_, new Size(), scale, scale,
				Imgproc.INTER_AREA);
		previewImage_ = new Mat(imageScaledToPreview_.size(), CvType.CV_8UC1);
		previewBitmap_ = Bitmap.createBitmap(imageScaledToPreview_.cols(),
				imageScaledToPreview_.rows(), Config.ARGB_8888);
	}

	private void step() throws InterruptedException {
		boolean previewDirty;
		boolean allLinesDirty;
		boolean needMoreLines;
		float blur;
		float threshold;
		Mode mode;
		synchronized (this) {
			while (true) {
				allLinesDirty = imageResidue_ == null || currentBlur_ != blur_;
				needMoreLines = lines_.isEmpty() || -lines_.lastKey() > threshold_
						&& lines_.size() < MAX_LINES;
				previewDirty = needMoreLines && mode_ == Mode.Vector || previewImage_ == null
						|| currentBlur_ != blur_ || currentThreshold_ != threshold_
						|| currentMode_ != mode_;
				if (needMoreLines || allLinesDirty || previewDirty || requestResult_) {
					break;
				}
				wait();
			}
			// Copy state.
			blur = blur_;
			threshold = threshold_;
			mode = mode_;
		}
		if (allLinesDirty) {
			initLines(blur);
		}
		if (needMoreLines) {
			generateLine(blur);
		}
		if (previewDirty) {
			generatePreview(blur, threshold, mode);
		}
		if (requestResult_ && !needMoreLines) {
			sendResult(blur, threshold);
		}
		currentMode_ = mode;
		currentBlur_ = blur;
		currentThreshold_ = threshold;
	}

	private void sendResult(float blur, float threshold) {
		// Generate thumbnail.
		renderPreview(blur, threshold, Mode.Vector);
		Mat thumbnail = new Mat(previewImage_.rows(), previewImage_.cols() * 2, CvType.CV_8U);
		imageScaledToPreview_.copyTo(thumbnail.colRange(0, previewImage_.cols()));
		previewImage_.copyTo(thumbnail.colRange(previewImage_.cols(), previewImage_.cols() * 2));
		Bitmap bmp = Bitmap.createBitmap(thumbnail.cols(), thumbnail.rows(), Config.ARGB_8888);
		Utils.matToBitmap(thumbnail, bmp);

		// Generate point array.
		Point[] points = (Point[]) lines_.subMap(lines_.firstKey(), -threshold + Float.MIN_VALUE)
				.values().toArray(new Point[0]);

		synchronized (this) {
			if (listener_ != null) {
				listener_.result(points,
						new Rect(0, 0, imageResidue_.cols(), imageResidue_.rows()), bmp);
			}
			requestResult_ = false;
		}
	}

	private void initLines(float blur) {
		// Resize to native resolution divided by blur factor.
		float scale = LINE_WIDTH_TO_IMAGE_WIDTH / blur / srcImage_.cols();
		Imgproc.resize(srcImage_, imageResidue_, new Size(), scale, scale, Imgproc.INTER_AREA);
		// Negative.
		final Mat scalar = new Mat(1, 1, CvType.CV_64FC1).setTo(new Scalar(255));
		Core.subtract(scalar, imageResidue_, imageResidue_);
		// Convert to S16.
		imageResidue_.convertTo(imageResidue_, CvType.CV_16SC1);
		// Full scale is now blur * GRAY_RESOLUTION.
		Core.multiply(imageResidue_, new Scalar(blur * GRAY_RESOLUTION / 255), imageResidue_);
		// Clear map.
		lines_.clear();
		// Put the first entry.
		Point p = new Point(random_.nextDouble() * imageResidue_.cols(), random_.nextDouble()
				* imageResidue_.rows());
		float residualDarkness = darkness(imageResidue_) / blur;
		lines_.put(-residualDarkness, p);
	}

	private void generateLine(float blur) {
		Point[] line;
		if (lines_.isEmpty()) {
			line = nextLine(imageResidue_, NUM_ATTEMPTS, null);
			float residualDarkness = darkness(imageResidue_) / blur;
			lines_.put(-residualDarkness, line[0]);
		} else {
			line = nextLine(imageResidue_, NUM_ATTEMPTS, lines_.get(lines_.lastKey()));
		}
		float residualDarkness = darkness(imageResidue_) / blur;
		lines_.put(-residualDarkness, line[1]);
		System.out.printf("Points: %d, darkness: %f\n", lines_.size(), residualDarkness);

		synchronized (this) {
			if (listener_ != null) {
				listener_.progress(residualDarkness, lines_.size());
			}
		}
	}

	private void generatePreview(float blur, float threshold, Mode mode) {
		renderPreview(blur, threshold, mode);
		Utils.matToBitmap(previewImage_, previewBitmap_);
		final Bitmap bmp = previewBitmap_;

		synchronized (this) {
			if (listener_ != null) {
				listener_.previewFrame(bmp);
			}
		}
	}

	private void renderPreview(float blur, float threshold, Mode mode) {
		if (mode == Mode.Raster) {
			// Gaussian blur
			if (blur > 0) {
				Imgproc.GaussianBlur(imageScaledToPreview_, previewImage_, new Size(), blur);
			} else {
				imageScaledToPreview_.assignTo(previewImage_);
			}

			// Simulate threshold
			Core.add(previewImage_, new Scalar(threshold * 255), previewImage_);
		} else {
			previewImage_.setTo(new Scalar(255));
			Point prevPoint = null;
			final Scalar black = new Scalar(0);
			for (Entry<Float, Point> e : lines_.entrySet()) {
				if (-e.getKey() < threshold) {
					break;
				}
				Point p = scalePoint(e.getValue(), blur);
				if (prevPoint != null) {
					Core.line(previewImage_, prevPoint, p, black);
				}
				prevPoint = p;
			}
		}
	}

	private static Point scalePoint(Point p, float scale) {
		return new Point(p.x * scale, p.y * scale);
	}

	private static InputStream resolveUri(Context context, Uri uri) throws IOException,
			MalformedURLException, FileNotFoundException {
		InputStream stream;
		if (uri.getScheme().startsWith("http")) {
			stream = new java.net.URL(uri.toString()).openStream();
		} else {
			stream = context.getContentResolver().openInputStream(uri);
		}
		return stream;
	}

	private static Mat streamToMat(InputStream stream) throws IOException {
		byte[] data = new byte[1024];
		MatOfByte chunk = new MatOfByte();
		MatOfByte buf = new MatOfByte();
		int read;
		while ((read = stream.read(data)) > 0) {
			chunk.fromArray(data);
			Mat subchunk = chunk.submat(0, read, 0, 1);
			buf.push_back(subchunk);
		}
		return buf;
	}

	/**
	 * Gets the best of several random lines.
	 * 
	 * The number of candidates is determined by the numAttempts argument. The criterion for
	 * determining the winner is the one which covers the highest average darkness in the image. As
	 * a side-effect, the winner will be subtracted from the image.
	 * 
	 * @param image
	 *            The image to approximate. Expected to be of floating point format, with higher
	 *            values representing darker areas. Should be scaled such that subtracting a value
	 *            of GRAY_RESOLUTION from a pixel corresponds to how much darkness a line going
	 *            through it adds. When the method returns, the winning line will be subtracted from
	 *            this image.
	 * @param numAttempts
	 *            How many candidates to examine.
	 * @param startPoint
	 *            Possibly, force the line to start at a certain point. In case of null, the line
	 *            will comprise two random point.
	 * @return The optimal line.
	 */
	private static Point[] nextLine(Mat image, int numAttempts, Point startPoint) {
		Mat mask = Mat.zeros(image.size(), CvType.CV_8U);
		Mat bestMask = Mat.zeros(image.size(), CvType.CV_8U);
		Point[] line = new Point[2];
		Point[] bestLine = null;
		double bestScore = Float.NEGATIVE_INFINITY;
		for (int i = 0; i < numAttempts; ++i) {
			generateRandomLine(image.size(), startPoint, line);

			mask.setTo(new Scalar(0));
			Core.line(mask, line[0], line[1], new Scalar(GRAY_RESOLUTION));

			double score = Core.mean(image, mask).val[0];
			if (score > bestScore) {
				bestScore = score;
				Mat t = mask;
				mask = bestMask;
				bestMask = t;
				bestLine = line.clone();
			}
		}
		Core.subtract(image, bestMask, image, bestMask, image.type());
		return bestLine;
	}

	private static void generateRandomLine(Size s, Point pStart, Point[] result) {
		if (pStart == null) {
			result[0] = new Point(random_.nextDouble() * s.width, random_.nextDouble() * s.height);
		} else {
			result[0] = pStart;
		}
		do {
			result[1] = new Point(random_.nextDouble() * s.width, random_.nextDouble() * s.height);
		} while (result[0].equals(result[1]));
	}

	private static float darkness(Mat in) {
		float total = (float) Core.sumElems(in).val[0];
		return total / in.cols() / in.rows() / 128;
	}
}

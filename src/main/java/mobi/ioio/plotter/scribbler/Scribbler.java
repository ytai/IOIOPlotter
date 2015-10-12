package mobi.ioio.plotter.scribbler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.net.Uri;

import mobi.ioio.plotter.MultiCurve;
import mobi.ioio.plotter.TransformedMultiCurve;
import mobi.ioio.plotter.shapes.ConcatMultiCurve;

public class Scribbler implements Runnable {
    public enum Mode {
        Raster, Vector
    }

    public interface Listener {
        void previewFrame(Bitmap frame, double totalTime);

        void progress(float darkness, int numLines);

        void result(MultiCurve curve, Bitmap thumbnail, double totalTime);
    }

    private static class CurveMapEntry {
        public final MultiCurve shape;
        public final Object context;
        public final double cumulativeTime;

        private CurveMapEntry(MultiCurve shape, Object context, double cumulativeTime) {
            this.shape = shape;
            this.context = context;
            this.cumulativeTime = cumulativeTime;
        }
    }

    private final Thread thread_;
    private final Context context_;
    private final Uri uri_;
    private float blur_;
    private float threshold_;
    private Mode mode_;
    private Listener listener_;
    private KernelFactory kernelFactory_;
    private boolean requestResult_ = false;
    private boolean requestReset_ = false;

    // Internals
    private static final float LINE_WIDTH_TO_IMAGE_WIDTH = 450;
    private static final float GRAY_RESOLUTION = 128;
    private static final int NUM_ATTEMPTS = 200;
    private static final int MAX_INSTANCES = 3000;
    private static Random random_ = new Random();
    private Mat srcImage_;
    private Mat imageScaledToPreview_;
    private Mat imageResidue_;
    private Mat previewImage_;
    private int previewImageCurveCount_;
    private float currentBlur_;
    private float currentThreshold_;
    private Mode currentMode_;
    private KernelFactory currentKernelFactory_;
    private SortedMap<Float, CurveMapEntry> curves_ = new TreeMap<Float, CurveMapEntry>();
    private Bitmap previewBitmap_;
    boolean stopped_ = false;

    public Scribbler(Context context, Uri uri, float initialBlur, float initialThershold,
                     Mode initialMode, Listener listener, KernelFactory initialFactory) {
        context_ = context;
        uri_ = uri;
        blur_ = initialBlur;
        threshold_ = initialThershold;
        mode_ = initialMode;
        listener_ = listener;
        kernelFactory_ = initialFactory;

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

    public synchronized void requestReset() {
        requestReset_ = true;
        notify();
    }

    public synchronized void setKernelFactory(KernelFactory factory) {
        kernelFactory_ = factory;
        notify();
    }

    @Override
    public void run() {
        try {
            load();
            while (!stopped_) {
                try {
                    step();
                } catch (InterruptedException ignored) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void load() throws IOException {
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
        boolean previewInvalid;
        boolean previewNeedsUpdate;
        boolean invalidateAll;
        boolean needMoreInstances;
        float blur;
        float threshold;
        KernelFactory factory;
        Mode mode;
        synchronized (this) {
            while (true) {
                invalidateAll = imageResidue_ == null || currentBlur_ != blur_
                        || currentKernelFactory_ != kernelFactory_ || requestReset_;
                needMoreInstances = invalidateAll || curves_.isEmpty() || -curves_.lastKey() > threshold_
                        && curves_.size() < MAX_INSTANCES;
                previewInvalid = invalidateAll || currentMode_ != mode_;
                previewNeedsUpdate = previewInvalid || needMoreInstances || currentThreshold_ != threshold_;
                if (needMoreInstances || invalidateAll || previewInvalid ||  previewNeedsUpdate
                        || requestResult_) {
                    break;
                }
                wait();
            }
            // Copy state.
            blur = blur_;
            threshold = threshold_;
            mode = mode_;
            factory = kernelFactory_;
        }
        if (invalidateAll) {
            clear(blur);
            requestReset_ = false;
        }
        if (needMoreInstances) {
            addKernelInstance(blur);
        }
        if (previewInvalid) {
            previewImage_.setTo(new Scalar(255));
            previewImageCurveCount_ = 0;
        }
        if (previewNeedsUpdate) {
            generatePreview(blur, threshold, mode);
        }
        if (requestResult_ && !needMoreInstances) {
            sendResult(blur, threshold);
        }
        currentMode_ = mode;
        currentBlur_ = blur;
        currentThreshold_ = threshold;
        currentKernelFactory_ = factory;
    }

    private void sendResult(float blur, float threshold) {
        // Generate thumbnail.
        renderPreview(blur, threshold, Mode.Vector);
        Mat thumbnail = new Mat(previewImage_.rows(), previewImage_.cols() * 2, CvType.CV_8U);
        imageScaledToPreview_.copyTo(thumbnail.colRange(0, previewImage_.cols()));
        previewImage_.copyTo(thumbnail.colRange(previewImage_.cols(), previewImage_.cols() * 2));
        Bitmap bmp = Bitmap.createBitmap(thumbnail.cols(), thumbnail.rows(), Config.ARGB_8888);
        Utils.matToBitmap(thumbnail, bmp);

        // Concatenate multi-curves.
        double totalTime = 0;
        ArrayList<MultiCurve> curves = new ArrayList<MultiCurve>(curves_.size());
        for (CurveMapEntry entry : curves_.headMap(-threshold + Float.MIN_VALUE).values()) {
            curves.add(entry.shape);
            totalTime = entry.cumulativeTime;
        }
        MultiCurve concat = new ConcatMultiCurve(curves);

        synchronized (this) {
            if (listener_ != null) {
                listener_.result(concat, bmp, totalTime);
            }
            requestResult_ = false;
        }
    }

    private void clear(float blur) {
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
        curves_.clear();

        kernelFactory_.setDimensions(imageResidue_.width(), imageResidue_.height());
    }

    private void addKernelInstance(float blur) {
        Object context = curves_.isEmpty() ? null : curves_.get(curves_.lastKey()).context;
        KernelInstance kernelInstance = nextKernalInstance(imageResidue_, NUM_ATTEMPTS, context);
        float residualDarkness = darkness(imageResidue_) / blur;
        double cumulativeTime = (curves_.isEmpty() ? 0 : curves_.get(curves_.lastKey()).cumulativeTime)
                + kernelInstance.shape_.totalTime();
        curves_.put(-residualDarkness, new CurveMapEntry(kernelInstance.shape_, kernelInstance.context_, cumulativeTime));
        System.out.printf("Points: %d, darkness: %f\n", curves_.size(), residualDarkness);

        synchronized (this) {
            if (listener_ != null) {
                listener_.progress(residualDarkness, curves_.size());
            }
        }
    }

    private void generatePreview(float blur, float threshold, Mode mode) {
        double totalTime = renderPreview(blur, threshold, mode);
        Utils.matToBitmap(previewImage_, previewBitmap_);
        final Bitmap bmp = previewBitmap_;

        synchronized (this) {
            if (listener_ != null) {
                listener_.previewFrame(bmp, totalTime);
            }
        }
    }

    private double renderPreview(float blur, float threshold, Mode mode) {
        // These are the relevant curves.
        SortedMap<Float, CurveMapEntry> curves = curves_.headMap(-threshold + Float.MIN_VALUE);

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
            // See if we can use a cached preview.
            if (curves.size() < previewImageCurveCount_) {
                previewImage_.setTo(new Scalar(255));
                previewImageCurveCount_ = 0;
            }

            final Scalar black = new Scalar(0);
            int i = 0;
            for (Entry<Float, CurveMapEntry> e : curves.entrySet()) {
                if (i++ < previewImageCurveCount_) continue;

                MultiCurve shape = new TransformedMultiCurve(e.getValue().shape,
                        new float[] { 0, 0},  blur, 1 / blur);
                shape.renderToMat(previewImage_, new Scalar(0));
            }
            previewImageCurveCount_ = curves.size();
        }

        if (curves.isEmpty()) return 0;
        return curves.get(curves.lastKey()).cumulativeTime;
    }

    private static Point scalePoint(Point p, float scale) {
        return new Point(p.x * scale, p.y * scale);
    }

    private static InputStream resolveUri(Context context, Uri uri) throws IOException {
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

    private KernelInstance nextKernalInstance(Mat image, int numAttempts, Object context) {
        Mat mask = Mat.zeros(image.size(), CvType.CV_8U);
        Mat bestMask = Mat.zeros(image.size(), CvType.CV_8U);
        KernelInstance bestInstance = null;
        double bestScore = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < numAttempts; ++i) {
            KernelInstance instance = kernelFactory_.createKernelInstance(context);

            mask.setTo(new Scalar(0));
            instance.shape_.renderToMat(mask, new Scalar(GRAY_RESOLUTION));

            double score = Core.mean(image, mask).val[0];
            if (score > bestScore) {
                bestScore = score;
                Mat t = mask;
                mask = bestMask;
                bestMask = t;
                bestInstance = instance;
            }
        }
        Core.subtract(image, bestMask, image, bestMask, image.type());
        return bestInstance;
    }

    private static float darkness(Mat in) {
        float total = (float) Core.sumElems(in).val[0];
        return total / in.cols() / in.rows() / 128;
    }
}

package mobi.ioio.plotter_app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import mobi.ioio.plotter.Plotter.MultiCurve;
import mobi.ioio.plotter.shapes.PointsCurve;
import mobi.ioio.plotter.shapes.SingleCurveMultiCurve;
import mobi.ioio.plotter_app.Scribbler.Mode;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class ScribblerActivity extends Activity implements OnClickListener {
	ImageView imageView_;
	TextView selectImageTextView_;
	TextView blurTextView_;
	TextView thresholdTextView_;
	TextView statusTextView_;
	SeekBar blurSeekBar_;
	SeekBar thresholdSeekBar_;
	CheckBox previewCheckbox_;
	Button doneButton_;

	private Scribbler scribbler_;

	private float darkness_ = 1;
	private int numLines_ = 0;
	private static final int GET_IMAGE_REQUEST_CODE = 100;
	private boolean donePressed_ = false;

	private static final String TAG = "ScribblerActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "Trying to load OpenCV library");
		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mOpenCVCallBack)) {
			Toast.makeText(this, "Cannot connect to OpenCV Manager", Toast.LENGTH_LONG).show();
			finish();
		}
	}

	@Override
	protected void onDestroy() {
		if (scribbler_ != null) {
			scribbler_.stop();
		}
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == GET_IMAGE_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				if (scribbler_ != null) {
					scribbler_.stop();
				}
				blurSeekBar_.setProgress(50);
				thresholdSeekBar_.setProgress(20);
				previewCheckbox_.setChecked(false);
				scribbler_ = new Scribbler(this, data.getData(), getBlur(), getThreshold(),
						getMode(), new Scribbler.Listener() {
							@Override
							public void previewFrame(final Bitmap frame) {
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										imageView_.setImageBitmap(frame);
									}
								});
							}

							@Override
							public void progress(final float darkness, final int numLines) {
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										updateProgress(darkness, numLines);
									}
								});
							}

							@Override
							public void result(final Point[] points, final Rect bounds,
									final Bitmap thumbnail) {
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										scribblerResult(points, bounds, thumbnail);
									}
								});
							}
						});
				selectImageTextView_.setVisibility(View.GONE);
				imageView_.setVisibility(View.VISIBLE);
			} else if (resultCode == RESULT_CANCELED) {
				// User cancelled the image capture
			} else {
				// Image capture failed, advise user
			}
		}
	}

	private Mode getMode() {
		return previewCheckbox_.isChecked() ? Mode.Vector : Mode.Raster;
	}

	private float getThreshold() {
		return thresholdSeekBar_.getProgress() / 100.f;
	}

	private float getBlur() {
		return blurSeekBar_.getProgress() / 10.f;
	}

	private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			if (status == LoaderCallbackInterface.SUCCESS) {
				ScribblerActivity.this.onManagerConnected();
			} else {
				super.onManagerConnected(status);
			}
		}
	};

	private class UpdateListener implements OnSeekBarChangeListener, OnCheckedChangeListener {
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			if (seekBar == thresholdSeekBar_) {
				thresholdTextView_.setText(String.format("%.0f%%", getThreshold() * 100));
				if (scribbler_ != null) {
					updateProgress(darkness_, numLines_);
					scribbler_.setThreshold(getThreshold());
				}
			} else if (seekBar == blurSeekBar_) {
				blurTextView_.setText(String.format("%.1f", getBlur()));
				if (scribbler_ != null) {
					doneButton_.setEnabled(false);
					scribbler_.setBlur(getBlur());
				}
			}
		}

		@Override
		public void onCheckedChanged(CompoundButton button, boolean value) {
			if (scribbler_ != null) {
				if (button == previewCheckbox_) {
					scribbler_.setMode(getMode());
				}
			}
		}
	}

	private UpdateListener updateListener_ = new UpdateListener();

	private void updateProgress(float darkness, int numLines) {
		darkness_ = darkness;
		numLines_ = numLines;
		doneButton_.setEnabled(!donePressed_ && darkness < getThreshold());
		statusTextView_.setText(String.format("%.0f%% (%d)", darkness * 100, numLines));
	}

	private void onManagerConnected() {
		Log.i(TAG, "OpenCV loaded successfully");

		setContentView(R.layout.activity_scribbler);

		imageView_ = (ImageView) findViewById(R.id.image);
		imageView_.setOnClickListener(ScribblerActivity.this);

		selectImageTextView_ = (TextView) findViewById(R.id.select_image);
		selectImageTextView_.setOnClickListener(this);

		blurSeekBar_ = (SeekBar) findViewById(R.id.blur);
		blurSeekBar_.setOnSeekBarChangeListener(updateListener_);

		thresholdSeekBar_ = (SeekBar) findViewById(R.id.threshold);
		thresholdSeekBar_.setOnSeekBarChangeListener(updateListener_);

		blurTextView_ = (TextView) findViewById(R.id.blurText);
		thresholdTextView_ = (TextView) findViewById(R.id.thresholdText);
		statusTextView_ = (TextView) findViewById(R.id.status);

		previewCheckbox_ = (CheckBox) findViewById(R.id.preview);
		previewCheckbox_.setOnCheckedChangeListener(updateListener_);

		doneButton_ = (Button) findViewById(R.id.done);
		doneButton_.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		if (view == imageView_) {
			AlertDialog dialog = new AlertDialog.Builder(ScribblerActivity.this)
					.setTitle("Confirm Action")
					.setMessage("Are you sure you want to replace the image?")
					.setPositiveButton("YES", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							doneButton_.setEnabled(false);
							selectImage();
						}
					}).setNegativeButton("NO", null).create();
			dialog.show();
		} else if (view == selectImageTextView_) {
			selectImage();
		} else if (view == doneButton_) {
			done();
		}
	}

	private void selectImage() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		startActivityForResult(intent, GET_IMAGE_REQUEST_CODE);
	}

	private void scribblerResult(Point[] points, Rect bounds, Bitmap thumbnail) {
		scribbler_.stop();
		try {
			// Write thumbnail file.
			File thumbnailFile = File.createTempFile("THUMB", ".png", getCacheDir());
			thumbnail.compress(CompressFormat.PNG, 100, new FileOutputStream(thumbnailFile));

			// Generate trace file.
			MultiCurve multiCurve = new SingleCurveMultiCurve(new PointsCurve(points),
					getBounds(bounds));
			File traceFile = File.createTempFile("TRACE", ".trc", getCacheDir());
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(traceFile));
			oos.writeObject(multiCurve);
			oos.close();

			Intent resultIntent = new Intent();
			resultIntent.setData(Uri.fromFile(traceFile));
			resultIntent.putExtra("thumbnail", Uri.fromFile(thumbnailFile));
			setResult(RESULT_OK, resultIntent);
		} catch (IOException e) {
			e.printStackTrace();
		}
		finish();
	}

	private static float[] getBounds(Rect bounds) {
		return new float[] { bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height };
	}

	private void done() {
		scribbler_.requestResult();
		donePressed_ = true;
		doneButton_.setEnabled(false);
		blurSeekBar_.setEnabled(false);
		thresholdSeekBar_.setEnabled(false);
		previewCheckbox_.setEnabled(false);
		imageView_.setEnabled(false);
	}
}

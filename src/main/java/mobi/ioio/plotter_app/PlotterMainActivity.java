package mobi.ioio.plotter_app;

import java.io.InputStream;
import java.io.ObjectInputStream;

import mobi.ioio.plotter.Plotter.MultiCurve;
import mobi.ioio.plotter.TransformedMultiCurve;
import mobi.ioio.plotter_app.PlotterService.IOIOBinder;
import mobi.ioio.plotter_app.PlotterService.Looper;
import mobi.ioio.plotter_app.PlotterService.State;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.zerokol.views.JoystickView;
import com.zerokol.views.JoystickView.OnJoystickMoveListener;

public class PlotterMainActivity extends Activity implements OnClickListener,
		OnJoystickMoveListener, ServiceConnection {
	private static final float MM_PER_SEC = 40;
	private static final float[] FULL_PAGE_BOUNDS = { 150, 330, 604, 850 };
	private static final float[] TOP_HALF_BOUNDS = { 150, 330, 604, 590 };
	private static final float[] BOTTOM_HALF_BOUNDS = { 150, 590, 604, 850 };
	private float[] pageBoundsMm_ = FULL_PAGE_BOUNDS;

	private static final int GET_PATH_REQUEST = 300;

	private BroadcastReceiver receiver_ = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			PlotterMainActivity.this.onReceive(intent);
		}
	};

	private PlotterService.State serviceState_ = State.DISCONNECTED;

	private TextView selectPathView_;
	private ImageView pathImageView_;
	private ToggleButton plotButton_;
	private Button homeButton_;
	private Button exitButton_;
	private Button stopButton_;
	private Looper looper_;
	private JoystickView joystick_;
	private Uri multiCurveUri_;
	private Uri thumbnailUri_;

	private boolean bound_ = false;

	@Override
	protected void onStart() {
		super.onStart();
		Intent intent = new Intent(this, PlotterService.class);
		startService(intent);
		bindService(intent, this, BIND_AUTO_CREATE);
		IntentFilter filter = new IntentFilter(PlotterService.ACTION_STATE_CHANGE);
		onReceive(registerReceiver(receiver_, filter));
	}

	protected void onReceive(Intent intent) {
		if (intent != null) {
			serviceState_ = PlotterService.State.values()[intent.getIntExtra(
					PlotterService.EXTRA_STATE, 0)];
		} else {
			serviceState_ = PlotterService.State.DISCONNECTED;
		}
		updateGui();
	}

	@Override
	protected void onStop() {
		if (bound_) {
			unbindService(this);
		}
		unregisterReceiver(receiver_);
		super.onStop();
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder binder) {
		looper_ = ((IOIOBinder) binder).getLooper();
		bound_ = true;
		Uri[] extra = (Uri[]) looper_.getExtra();
		if (extra != null && multiCurveUri_ == null) {
			multiCurveUri_ = extra[0];
			thumbnailUri_ = extra[1];
		}
		updateGui();
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		looper_ = null;
		updateGui();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_plotter_main);

		selectPathView_ = (TextView) findViewById(R.id.select_path);
		selectPathView_.setOnClickListener(this);

		pathImageView_ = (ImageView) findViewById(R.id.path);
		pathImageView_.setOnClickListener(this);

		plotButton_ = (ToggleButton) findViewById(R.id.plot_button);
		plotButton_.setOnClickListener(this);

		homeButton_ = (Button) findViewById(R.id.home_button);
		homeButton_.setOnClickListener(this);

		exitButton_ = (Button) findViewById(R.id.exit_button);
		exitButton_.setOnClickListener(this);

		stopButton_ = (Button) findViewById(R.id.stop_button);
		stopButton_.setOnClickListener(this);

		joystick_ = (JoystickView) findViewById(R.id.joystick);
		joystick_.setOnJoystickMoveListener(this, 20);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.plotter_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.page_size:
			setPageSize();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void setPageSize() {
		Builder builder = new AlertDialog.Builder(this);
		View view = getLayoutInflater().inflate(R.layout.dialog_page_size, null);
		final RadioGroup group = (RadioGroup) view.findViewById(R.id.select_size);
		final EditText[] bounds = new EditText[] { (EditText) view.findViewById(R.id.xmin),
				(EditText) view.findViewById(R.id.ymin), (EditText) view.findViewById(R.id.xmax),
				(EditText) view.findViewById(R.id.ymax) };

		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (group.getCheckedRadioButtonId()) {
				case R.id.full_page:
					pageBoundsMm_ = FULL_PAGE_BOUNDS;
					break;

				case R.id.top_half:
					pageBoundsMm_ = TOP_HALF_BOUNDS;
					break;

				case R.id.bottom_half:
					pageBoundsMm_ = BOTTOM_HALF_BOUNDS;
					break;

				case R.id.custom_size:
					pageBoundsMm_ = new float[4];
					for (int i = 0; i < 4; ++i) {
						pageBoundsMm_[i] = Float.valueOf(bounds[i].getText().toString());
					}
					break;
				}
			}
		};

		group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				final boolean enable = checkedId == R.id.custom_size;
				for (EditText bound : bounds) {
					bound.setEnabled(enable);
				}
			}
		});

		builder.setTitle("Page Size").setView(view).setPositiveButton("OK", listener)
				.setNegativeButton("Cancel", null).create().show();
	}

	@Override
	public void onClick(View v) {
		if (v == pathImageView_ || v == selectPathView_) {
			final Intent edgeTracerIntent = new Intent();
			edgeTracerIntent.setClass(this, EdgeTracerActivity.class);
			final Intent scribblerIntent = new Intent();
			scribblerIntent.setClass(this, ScribblerActivity.class);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Select path type").setItems(
					new CharSequence[] { "Edge Tracer", "Scribbler" },
					new AlertDialog.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								startActivityForResult(edgeTracerIntent, GET_PATH_REQUEST);
								break;
							case 1:
								startActivityForResult(scribblerIntent, GET_PATH_REQUEST);
								break;
							}
						}
					}).create().show();
		} else if (v == plotButton_) {
			try {
				if (plotButton_.isChecked() && serviceState_ == State.STOPPED) {
					InputStream inputStream = getContentResolver().openInputStream(multiCurveUri_);
					ObjectInputStream ois = new ObjectInputStream(inputStream);
					MultiCurve multiCurve = (MultiCurve) ois.readObject();
					looper_.setPath(transform(multiCurve), new Uri[] { multiCurveUri_,
							thumbnailUri_ });
				}
				looper_.setTargetState(plotButton_.isChecked() ? State.PLOTTING : State.PAUSED);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (v == stopButton_) {
			looper_.setTargetState(State.STOPPED);
		} else if (v == homeButton_) {
			looper_.home();
		} else if (v == exitButton_) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Are you sure?").setTitle("Confirm Exit");
			builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					exit();
				}
			});
			builder.setNegativeButton("No", null);
			builder.create().show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == GET_PATH_REQUEST) {
			if (resultCode == RESULT_OK) {
				try {
					multiCurveUri_ = data.getData();
					thumbnailUri_ = (Uri) data.getParcelableExtra("thumbnail");
					updateGui();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void updateGui() {
		final boolean hasPath = multiCurveUri_ != null;

		plotButton_.setEnabled(bound_ && hasPath
				&& serviceState_ != PlotterService.State.DISCONNECTED);
		plotButton_.setChecked(bound_ && hasPath && serviceState_ == PlotterService.State.PLOTTING);

		homeButton_.setEnabled(bound_ && serviceState_ == PlotterService.State.STOPPED);
		exitButton_.setEnabled(bound_);
		stopButton_.setEnabled(bound_ && hasPath
				&& serviceState_ != PlotterService.State.DISCONNECTED);
		joystick_.setEnabled(bound_ && serviceState_ == PlotterService.State.STOPPED);

		pathImageView_.setVisibility(hasPath ? View.VISIBLE : View.GONE);
		pathImageView_.setImageURI(thumbnailUri_);
		selectPathView_.setVisibility(hasPath ? View.GONE : View.VISIBLE);

		pathImageView_.setEnabled(!bound_ || serviceState_ == PlotterService.State.DISCONNECTED
				|| serviceState_ == State.STOPPED);
		selectPathView_.setEnabled(!bound_ || serviceState_ == PlotterService.State.DISCONNECTED
				|| serviceState_ == State.STOPPED);
	}

	private void exit() {
		Intent intent = new Intent(this, PlotterService.class);
		stopService(intent);
		finish();
	}

	static final float C = (float) Math.cos(Math.PI / 4);

	@Override
	public void onValueChanged(float x, float y) {
		final float l = x * C + y * C;
		final float r = -x * C + y * C;
		looper_.setManualSpeed(l, r);
	}

	private MultiCurve transform(MultiCurve multiCurve) {
		float[] plotBounds = multiCurve.getBounds();
		final float plotWidth = plotBounds[2] - plotBounds[0];
		final float plotHeight = plotBounds[3] - plotBounds[1];

		final float pageWidth = pageBoundsMm_[2] - pageBoundsMm_[0];
		final float pageHeight = pageBoundsMm_[3] - pageBoundsMm_[1];

		final float scale = Math.min(pageWidth / plotWidth, pageHeight / plotHeight);

		final float pageCenterX = pageBoundsMm_[0] + pageWidth / 2;
		final float pageCenterY = pageBoundsMm_[1] + pageHeight / 2;

		final float plotCenterX = plotBounds[0] + plotWidth / 2;
		final float plotCenterY = plotBounds[1] + plotHeight / 2;

		final float[] offset = { pageCenterX - plotCenterX * scale,
				pageCenterY - plotCenterY * scale };

		return new TransformedMultiCurve(multiCurve, offset, scale, 1.f / MM_PER_SEC);
	}
}

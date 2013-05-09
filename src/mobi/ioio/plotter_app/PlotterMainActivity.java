package mobi.ioio.plotter_app;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.Sequencer;
import ioio.lib.api.Sequencer.ChannelConfig;
import ioio.lib.api.Sequencer.ChannelConfigBinary;
import ioio.lib.api.Sequencer.ChannelConfigPwmPosition;
import ioio.lib.api.Sequencer.ChannelConfigSteps;
import ioio.lib.api.Sequencer.ChannelCue;
import ioio.lib.api.Sequencer.ChannelCueBinary;
import ioio.lib.api.Sequencer.ChannelCuePwmPosition;
import ioio.lib.api.Sequencer.ChannelCueSteps;
import ioio.lib.api.Sequencer.Clock;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.io.InputStream;
import java.io.ObjectInputStream;

import mobi.ioio.plotter.Plotter;
import mobi.ioio.plotter.Plotter.MultiCurve;
import mobi.ioio.plotter.TransformedMultiCurve;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.zerokol.views.JoystickView;
import com.zerokol.views.JoystickView.OnJoystickMoveListener;

public class PlotterMainActivity extends IOIOActivity implements OnClickListener, OnJoystickMoveListener {
	private static final float[] HOME = { 375, 285 };

	private static final int GET_PATH_REQUEST = 300;

	private ImageView pathImageView_;
	private ToggleButton plotButton_;
	private Button homeButton_;
	private Looper looper_;
	private JoystickView joystick_;

	private Uri mutiCurveUri_;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_plotter_main);

		pathImageView_ = (ImageView) findViewById(R.id.path);
		pathImageView_.setOnClickListener(this);

		plotButton_ = (ToggleButton) findViewById(R.id.plot_button);
		plotButton_.setOnClickListener(this);

		homeButton_ = (Button) findViewById(R.id.home_button);
		homeButton_.setOnClickListener(this);

		joystick_ = (JoystickView) findViewById(R.id.joystick);
		joystick_.setOnJoystickMoveListener(this, 20);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.plotter_main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		if (v == pathImageView_) {
			Intent intent = new Intent();
			intent.setClass(this, EdgeTracerActivity.class);
			startActivityForResult(intent, GET_PATH_REQUEST);
		} else if (v == plotButton_) {
			looper_.setTargetState(plotButton_.isChecked() ? State.PLOTTING : State.STOPPED);
		} else if (v == homeButton_) {
			looper_.setPosition(HOME);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == GET_PATH_REQUEST) {
			if (resultCode == RESULT_OK) {
				try {
					mutiCurveUri_ = data.getData();
					pathImageView_.setImageURI((Uri) data.getParcelableExtra("thumbnail"));
					plotButton_.setEnabled(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	enum State {
		STOPPED, PLOTTING
	}

	private class Looper extends BaseIOIOLooper {
		private static final float MANUAL_SEC_PER_TICK = 10e-3f;
		private static final float MANUAL_MM_PER_SEC = 50.f;
		private static final float MM_PER_SEC = 30;

		private final ChannelConfigSteps stepper1StepConfig_ = new ChannelConfigSteps(new DigitalOutput.Spec(10));
		private final ChannelConfigBinary stepper1DirConfig_ = new ChannelConfigBinary(false, false,
				new DigitalOutput.Spec(11));
		private final ChannelConfigSteps stepper2StepConfig_ = new ChannelConfigSteps(new DigitalOutput.Spec(12));
		private final ChannelConfigBinary stepper2DirConfig_ = new ChannelConfigBinary(false, false,
				new DigitalOutput.Spec(13));
		private final ChannelConfigPwmPosition servoConfig_ = new ChannelConfigPwmPosition(Clock.CLK_2M, 40000, 2000,
				new DigitalOutput.Spec(46));

		private final ChannelConfig[] config_ = new ChannelConfig[] { stepper1StepConfig_, stepper1DirConfig_,
				stepper2StepConfig_, stepper2DirConfig_, servoConfig_ };

		private ChannelCueSteps stepper1StepCue_ = new ChannelCueSteps();
		private ChannelCueBinary stepper1DirCue_ = new ChannelCueBinary();
		private ChannelCueSteps stepper2StepCue_ = new ChannelCueSteps();
		private ChannelCueBinary stepper2DirCue_ = new ChannelCueBinary();
		private ChannelCuePwmPosition servoCue_ = new ChannelCuePwmPosition();

		private ChannelCueSteps[] stepperStepCues_ = new ChannelCueSteps[] { stepper1StepCue_, stepper2StepCue_ };
		private ChannelCueBinary[] stepperDirCues_ = new ChannelCueBinary[] { stepper1DirCue_, stepper2DirCue_ };

		private final ChannelCue[] cue_ = new ChannelCue[] { stepper1StepCue_, stepper1DirCue_, stepper2StepCue_,
				stepper2DirCue_, servoCue_ };

		private Plotter plotter_ = new Plotter(1e-3f);

		private Sequencer sequencer_;

		State currentState_ = State.STOPPED;
		State targetState_ = State.STOPPED;
		private float[] manualSpeed_ = new float[] { 0, 0 };

		@Override
		protected void setup() throws ConnectionLostException, InterruptedException {
			toast("IOIO Connected");
			sequencer_ = ioio_.openSequencer(config_);
			sequencer_.start();
			servoCue_.pulseWidth = 2000;
			setPosition(HOME);
		}

		public synchronized void setPosition(float[] pos) {
			plotter_.setXy(pos[0], pos[1]);
		}

		public void setTargetState(State state) {
			targetState_ = state;
		}

		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
			switch (currentState_) {
			case STOPPED:
				switch (targetState_) {
				case STOPPED:
					manualMode();
					break;

				case PLOTTING:
					startPlot();
					break;
				}
				break;

			case PLOTTING:
				switch (targetState_) {
				case STOPPED:
					stopPlot();
					break;

				case PLOTTING:
					keepPlotting();
					break;
				}
				break;
			}
			Thread.sleep(5);
		}

		private void startPlot() throws ConnectionLostException {
			InputStream inputStream;
			try {
				inputStream = getContentResolver().openInputStream(mutiCurveUri_);
				ObjectInputStream ois = new ObjectInputStream(inputStream);
				MultiCurve multiCurve = (MultiCurve) ois.readObject();
				plotter_.setMultiCurve(transform(multiCurve));
				currentState_ = State.PLOTTING;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void stopPlot() throws ConnectionLostException {
			sequencer_.stop();
			currentState_ = State.STOPPED;
			sequencer_.start();
		}

		private void keepPlotting() throws ConnectionLostException, InterruptedException {
			while (sequencer_.available() > 0) {
				int time = plotter_.nextSegment(stepperStepCues_, stepperDirCues_, servoCue_);
				sequencer_.push(cue_, time);
			}
		}

		private void manualMode() throws ConnectionLostException, InterruptedException {
			if (sequencer_.available() >= 31) {
				int time = plotter_.manualDelta(manualSpeed_, MANUAL_SEC_PER_TICK, stepperStepCues_, stepperDirCues_);
				sequencer_.push(cue_, time);
			}
		}

		@Override
		public void disconnected() {
			toast("IOIO Disconnected");
		}

		private void toast(final String msg) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(PlotterMainActivity.this, msg, Toast.LENGTH_LONG).show();
				}
			});
		}

		public void setManualSpeed(float x, float y) {
			manualSpeed_ = new float[] { x * MANUAL_MM_PER_SEC * MANUAL_SEC_PER_TICK,
					y * MANUAL_MM_PER_SEC * MANUAL_SEC_PER_TICK };
		}

		private MultiCurve transform(MultiCurve multiCurve) {
			return new TransformedMultiCurve(multiCurve, new float[] { 120, 330 }, 0.25f, 1.f / MM_PER_SEC);
		}
	}

	@Override
	public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
		looper_ = new Looper();
		return looper_;
	}

	static final float C = (float) Math.cos(Math.PI / 4);

	@Override
	public void onValueChanged(float x, float y) {
		final float l = x * C + y * C;
		final float r = -x * C + y * C;
		looper_.setManualSpeed(l, r);
	}

}

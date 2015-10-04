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
import ioio.lib.util.android.IOIOService;
import mobi.ioio.plotter.Plotter;
import mobi.ioio.plotter.Plotter.MultiCurve;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

public class PlotterService extends IOIOService {
	public enum State {
		DISCONNECTED,
		STOPPED,
		PAUSED,
		PLOTTING
	}

	private static final int PLOTTER_NOTIFICATION = 1;

	private Looper looper_;
	public IBinder binder_ = new IOIOBinder();

	private Handler handler_;
	
	public class IOIOBinder extends Binder {
		public Looper getLooper() {
			return looper_;
		}
	};

	@Override
	protected IOIOLooper createIOIOLooper() {
		looper_ = new Looper();
		return looper_;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Notification notification = new Notification(R.drawable.ic_launcher, "IOIO Plotter",
		        System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, PlotterMainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		handler_ = new Handler(android.os.Looper.getMainLooper());
		notification.setLatestEventInfo(this, "IOIO Plotter", "Disconnected.", pendingIntent);
		startForeground(PLOTTER_NOTIFICATION, notification);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder_;
	}

	private static final float[] HOME = { 375, 285 };
	public static final String ACTION_STATE_CHANGE = "mobi.ioio.plotter.state_changed";
	public static final String EXTRA_STATE = "state";

	public class Looper extends BaseIOIOLooper {
		private static final float MANUAL_SEC_PER_TICK = 10e-3f;
		private static final float MANUAL_MM_PER_SEC = 50.f;
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
	
		private State currentState_;
		private State targetState_ = State.STOPPED;
		private float[] manualSpeed_ = new float[] { 0, 0 };
		private MultiCurve multiCurve_;
		private Object extra_;
		
		public Looper() {
			home();
			setCurrentState(State.DISCONNECTED);
		}
	
		@Override
		protected void setup() throws ConnectionLostException, InterruptedException {
			toast("IOIO Connected");
			sequencer_ = ioio_.openSequencer(config_);
			sequencer_.start();
			servoCue_.pulseWidth = 2350;
			setCurrentState(State.STOPPED);
		}
	
		public void home() {
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
					
				case PAUSED:
				case PLOTTING:
					preparePlot();
					break;
				}
				break;

			case PAUSED:
				switch (targetState_) {
				case STOPPED:
					stopPlot();
					break;
					
				case PAUSED:
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
					
				case PAUSED:
					pausePlot();
					break;
	
				case PLOTTING:
					fillBuffer();
					break;
				}
				break;
			}
			Thread.sleep(5);
		}
	
		public void setPath(MultiCurve multiCurve, Object extra) {
			extra_ = extra;
			multiCurve_ = multiCurve;
		}
		
		public Object getExtra() {
			return extra_;
		}
	
		private void preparePlot() throws ConnectionLostException, InterruptedException {
			sequencer_.stop();
			plotter_.setMultiCurve(multiCurve_);
			fillBuffer();
			setCurrentState(State.PAUSED);
		}
		
		private void startPlot() throws ConnectionLostException {
			sequencer_.start();
			setCurrentState(State.PLOTTING);
		}
	
		private void stopPlot() throws ConnectionLostException {
			sequencer_.stop();
			setCurrentState(State.STOPPED);
			sequencer_.start();
		}
		
		private void pausePlot() throws ConnectionLostException {
			sequencer_.pause();
			setCurrentState(State.PAUSED);
		}
	
		private void setCurrentState(State state) {
			currentState_ = state;
			Intent intent = new Intent(ACTION_STATE_CHANGE);
			intent.putExtra(EXTRA_STATE, state.ordinal());
			sendStickyBroadcast(intent);
			
			Notification notification = new Notification(R.drawable.ic_launcher, "IOIO Plotter",
			        System.currentTimeMillis());
			Intent notificationIntent = new Intent(PlotterService.this, PlotterMainActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(PlotterService.this, 0, notificationIntent, 0);
			notification.setLatestEventInfo(PlotterService.this, "IOIO Plotter", state.toString(), pendingIntent);
			
			startForeground(PLOTTER_NOTIFICATION, notification);
		}

		private void fillBuffer() throws ConnectionLostException, InterruptedException {
			while (sequencer_.available() > 0) {
				int time = plotter_.nextSegment(stepperStepCues_, stepperDirCues_, servoCue_);
				if (time > 0) {
					sequencer_.push(cue_, time);
				} else {
					setTargetState(State.STOPPED);
					setCurrentState(State.STOPPED);
					toast("Done");
					return;
				}
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
			setCurrentState(State.DISCONNECTED);
		}
	
		private void toast(final String msg) {
			handler_.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(PlotterService.this, msg, Toast.LENGTH_LONG).show();
				}
			});
		}
	
		public void setManualSpeed(float x, float y) {
			manualSpeed_ = new float[] { x * MANUAL_MM_PER_SEC * MANUAL_SEC_PER_TICK,
					y * MANUAL_MM_PER_SEC * MANUAL_SEC_PER_TICK };
		}

		@Override
		public void incompatible() {
			// TODO Auto-generated method stub
			
		}
	}
}
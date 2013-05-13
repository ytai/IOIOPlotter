package com.zerokol.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class JoystickView extends View implements Runnable {
	// Constants
	public final static long DEFAULT_LOOP_INTERVAL = 100; // 100 ms
	public final static int FRONT = 3;
	public final static int FRONT_RIGHT = 4;
	public final static int RIGHT = 5;
	public final static int RIGHT_BOTTOM = 6;
	public final static int BOTTOM = 7;
	public final static int BOTTOM_LEFT = 8;
	public final static int LEFT = 1;
	public final static int LEFT_FRONT = 2;
	// Variables
	private OnJoystickMoveListener onJoystickMoveListener; // Listener
	private Thread thread = new Thread(this);
	private long loopInterval = DEFAULT_LOOP_INTERVAL;
	private int xPosition = 0; // Touch x position
	private int yPosition = 0; // Touch y position
	private int centerX = 0; // Center view x position
	private int centerY = 0; // Center view y position
	private Paint mainCircle;
	private Paint button;
	private Paint buttonDisabled;
	private Paint axisLine;
	private int joystickRadius;
	private int buttonRadius;

	public JoystickView(Context context) {
		super(context);
	}

	public JoystickView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initJoystickView();
	}

	public JoystickView(Context context, AttributeSet attrs, int defaultStyle) {
		super(context, attrs, defaultStyle);
		initJoystickView();
	}

	protected void initJoystickView() {
		mainCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
		mainCircle.setColor(Color.LTGRAY);
		mainCircle.setStyle(Paint.Style.FILL_AND_STROKE);

		axisLine = new Paint();
		axisLine.setStrokeWidth(2);
		axisLine.setColor(Color.BLACK);

		button = new Paint(Paint.ANTI_ALIAS_FLAG);
		button.setColor(Color.GRAY);
		button.setStyle(Paint.Style.FILL);

		buttonDisabled = new Paint(Paint.ANTI_ALIAS_FLAG);
		buttonDisabled.setColor(Color.LTGRAY);
		buttonDisabled.setStyle(Paint.Style.FILL);
	}

	@Override
	protected void onFinishInflate() {
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// setting the measured values to resize the view to a certain width and
		// height
		int d = Math.min(measure(widthMeasureSpec), measure(heightMeasureSpec));

		setMeasuredDimension(d, d);

		buttonRadius = (int) (d / 2 * 0.25);
		joystickRadius = (int) (d / 2 * 0.75);
	}

	private static int measure(int measureSpec) {
		if (MeasureSpec.getMode(measureSpec) == MeasureSpec.UNSPECIFIED) {
			// Return a default size of 200 if no bounds are specified.
			return 200;
		}
		return MeasureSpec.getSize(measureSpec);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		centerX = getWidth() / 2;
		centerY = getHeight() / 2;

		// Main circle
		canvas.drawCircle((int) centerX, (int) centerY, Math.min(getWidth(), getHeight()) / 2, mainCircle);

		// Cross
		canvas.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2, axisLine);
		canvas.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight(), axisLine);

		// Move button
		canvas.drawCircle(centerX + xPosition, centerY + yPosition, buttonRadius, isEnabled() ? button : buttonDisabled);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return true;
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			xPosition = 0;
			yPosition = 0;
			thread.interrupt();
			if (onJoystickMoveListener != null) {
				onJoystickMoveListener.onValueChanged(posX(), posY());
			}
		} else {
			float x = event.getX() - centerX;
			float y = event.getY() - centerY;
			float len = (float) Math.hypot(x, y) / joystickRadius;
			if (len > 1) {
				x /= len;
				y /= len;
			}
			xPosition = (int) x;
			yPosition = (int) y;
		}
		invalidate();
		if (onJoystickMoveListener != null && event.getAction() == MotionEvent.ACTION_DOWN) {
			if (thread != null && thread.isAlive()) {
				thread.interrupt();
			}
			thread = new Thread(this);
			thread.start();
			onJoystickMoveListener.onValueChanged(posX(), posY());
		}
		return true;
	}

	private float posX() {
		return (float) xPosition / joystickRadius;
	}

	private float posY() {
		return (float) yPosition / joystickRadius;
	}

	public void setOnJoystickMoveListener(OnJoystickMoveListener listener, long repeatInterval) {
		this.onJoystickMoveListener = listener;
		this.loopInterval = repeatInterval;
	}

	public static interface OnJoystickMoveListener {
		public void onValueChanged(float x, float y);
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			post(new Runnable() {
				@Override
				public void run() {
					onJoystickMoveListener.onValueChanged(posX(), posY());
				}
			});
			try {
				Thread.sleep(loopInterval);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
}
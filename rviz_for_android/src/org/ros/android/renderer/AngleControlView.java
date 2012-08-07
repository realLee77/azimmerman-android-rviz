package org.ros.android.renderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class AngleControlView extends View implements android.view.GestureDetector.OnGestureListener {
	
	public static interface OnAngleChangeListener {
		public void angleChange(float newAngle, float delta); 
	}
	
	private static Paint paint;

	private GestureDetector gestureDetector;

	private static Paint createDefaultPaint() {
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		return paint;
	}

	public AngleControlView(Context context) {
		super(context);
		init();
	}

	public AngleControlView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public AngleControlView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		paint = createDefaultPaint();
		paint.setAntiAlias(true);
		paint.setColor(Color.BLUE);
		paint.setTextSize(40);
		paint.setStrokeWidth(0);
		paint.setStyle(Paint.Style.STROKE);

		gestureDetector = new GestureDetector(getContext(), this);
	}

	private static final int KNOB_RADIUS = 150;
	private static final int KNOB_LINEWIDTH = 30;

	private static final int INNER_RADIUS = KNOB_RADIUS - (KNOB_LINEWIDTH / 2) + 3;
	private static final int OUTER_RADIUS = KNOB_RADIUS + (KNOB_LINEWIDTH / 2) - 3;

	private static final int ARROW_DX = 10;
	private static final int ARROW_DY = 15;
	private static final int KNOB_WIDTH = (2 * KNOB_RADIUS) + KNOB_LINEWIDTH + 12;

	private static final double TICK_SEPARATION = Math.toRadians(10); // Degrees

	private static final int COLOR_BACKGROUND = Color.GRAY;
	private static final int COLOR_ARROW = Color.BLACK;
	private static final int COLOR_TICKS = Color.DKGRAY;

	private int centerX;
	private int centerY;
	private float angle = 0f;
	private RectF arcRect = new RectF();	

	private static final OnAngleChangeListener DEFAULT_LISTENER = new OnAngleChangeListener() {
		@Override
		public void angleChange(float newAngle, float delta) {
		}
	};
	
	private OnAngleChangeListener angleListener = DEFAULT_LISTENER;
	
	public void setOnAngleChangeListener(OnAngleChangeListener angleListener) {
		this.angleListener = angleListener;
	}
	
	@Override
	protected void onMeasure(int widthSpec, int heightSpec) {

		int measuredWidth = MeasureSpec.getSize(widthSpec);

		int measuredHeight = MeasureSpec.getSize(heightSpec);

		/*measuredWidth and measured height are your view boundaries. You need to change these values based on your requirement E.g.

		if you want to draw a circle which fills the entire view, you need to select the Min(measuredWidth,measureHeight) as the radius.

		Now the boundary of your view is the radius itself i.e. height = width = radius. */

		/* After obtaining the height, width of your view and performing some changes you need to set the processed value as your view dimension by using the method setMeasuredDimension */

		int d = Math.min(measuredWidth, measuredHeight);
		setMeasuredDimension(KNOB_WIDTH, KNOB_WIDTH);

		/* If you consider drawing circle as an example, you need to select the minimum of height and width and set that value as your screen dimensions

		int d=Math.min(measuredWidth, measuredHeight);

		setMeasuredDimension(d,d); */

		int height = getMeasuredHeight();
		int width = getMeasuredWidth();
		centerX = width / 2;
		centerY = width / 2;
		arcRect.set(centerX - KNOB_RADIUS, centerY - KNOB_RADIUS, centerX + KNOB_RADIUS, centerY + KNOB_RADIUS);

		Log.i("SIzing", "done");
	}

	@Override
	protected void onDraw(Canvas canvas) {
		paint.setStrokeWidth(5);
		paint.setColor(Color.DKGRAY);
		canvas.drawCircle(centerX+3, centerY+2, INNER_RADIUS, paint);
		canvas.drawCircle(centerX+3, centerY+2, OUTER_RADIUS, paint);
		
		canvas.rotate(angle, centerX, centerY);

		paint.setStrokeWidth(KNOB_LINEWIDTH);
		paint.setColor(COLOR_BACKGROUND);
		canvas.drawCircle(centerX, centerY, KNOB_RADIUS, paint);

		paint.setStrokeWidth(1);
		paint.setColor(Color.BLACK);
		canvas.drawCircle(centerX, centerY, KNOB_RADIUS + (KNOB_LINEWIDTH / 2), paint);
		canvas.drawCircle(centerX, centerY, KNOB_RADIUS - (KNOB_LINEWIDTH / 2), paint);

		paint.setColor(COLOR_TICKS);
		for(float i = 0; i < 2 * Math.PI; i += TICK_SEPARATION) {
			float cos = FloatMath.cos(i);
			float sin = FloatMath.sin(i);
			canvas.drawLine(centerX + INNER_RADIUS * cos, centerY + INNER_RADIUS * sin, centerX + OUTER_RADIUS * cos, centerY + OUTER_RADIUS * sin, paint);
		}

		paint.setStrokeWidth(3);
		paint.setColor(COLOR_ARROW);
		canvas.drawArc(arcRect, -17, 35, false, paint);
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.rotate(-17,centerX,centerY);
		canvas.drawLine(centerX + KNOB_RADIUS, centerY, centerX + KNOB_RADIUS + ARROW_DX, centerY + ARROW_DY, paint);
		canvas.drawLine(centerX + KNOB_RADIUS, centerY, centerX + KNOB_RADIUS - ARROW_DX, centerY + ARROW_DY, paint);
		canvas.restore();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(gestureDetector.onTouchEvent(event)) {
			return true;
		} else {
			return super.onTouchEvent(event);
		}
	}

	private float dragStartDeg;

	@Override
	public boolean onDown(MotionEvent e) {
		dragStartDeg = toDegrees(e.getX(), e.getY());
		return (dragStartDeg >= 0);
	}

	private float toDegrees(float x, float y) {
		float radius = (float) Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));

		if(radius < (KNOB_RADIUS - 1.5 * KNOB_LINEWIDTH))
			return -999;

		return (float) -(Math.toDegrees(Math.atan2(centerY - y, centerX - x)) - 180);
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		if(dragStartDeg >= 0) {
			float currentAngle = toDegrees(e2.getX(), e2.getY());

			float delta = Utility.cap(dragStartDeg - currentAngle, -20f, 20f);

			angle += delta;
			angle %= 360.0;
			dragStartDeg = currentAngle;
			this.invalidate();
			angleListener.angleChange(angle, delta);
		}
		return true;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

}

package org.ros.android.renderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;

public class TranslationControlView extends View implements OnGestureListener {

	private Paint paint;

	private GestureDetector gestureDetector;

	public interface OnMoveListener {
		public void onMove(float dX, float dY);
	}

	private static final OnMoveListener DEFAULT_LISTENER = new OnMoveListener() {
		@Override
		public void onMove(float dX, float dY) {
		}
	};
	private OnMoveListener oml = DEFAULT_LISTENER;

	public TranslationControlView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public TranslationControlView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public TranslationControlView(Context context) {
		super(context);
		init();
	}

	private static Paint createDefaultPaint() {
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		return paint;
	}

	private void init() {
		paint = createDefaultPaint();
		paint.setAntiAlias(true);
		paint.setColor(Color.BLUE);
		paint.setStrokeWidth(0);
		paint.setStyle(Paint.Style.STROKE);

		gestureDetector = new GestureDetector(getContext(), this);
		gestureDetector.setIsLongpressEnabled(false);
	}

	public void setOnMoveListener(OnMoveListener listener) {
		oml = listener;
	}

	private static final int BUTTON_WIDTH = 55;
	private static final int BUTTON_HEIGHT = 40;
	private static final int LINE_SPACING = 11;
	private static final int LINE_HEIGHT_GAP = 10;

	private static final RectF buttonRect = new RectF(0f, 0f, BUTTON_WIDTH, BUTTON_HEIGHT);

	@Override
	protected void onMeasure(int widthSpec, int heightSpec) {
//		int measuredWidth = MeasureSpec.getSize(widthSpec);
//		int measuredHeight = MeasureSpec.getSize(heightSpec);
		setMeasuredDimension(BUTTON_WIDTH, BUTTON_HEIGHT);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.GRAY);
		canvas.drawRoundRect(buttonRect, 5, 5, paint);

		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(Color.BLACK);
		canvas.drawRoundRect(buttonRect, 5, 5, paint);

		for(int x = LINE_SPACING; x <= BUTTON_WIDTH - LINE_SPACING; x += LINE_SPACING)
			canvas.drawLine(x, LINE_HEIGHT_GAP, x, BUTTON_HEIGHT - LINE_HEIGHT_GAP, paint);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		Log.e("ON DOWN", "ON DOWN");
		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		oml.onMove(distanceX, distanceY);
		return true;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(gestureDetector.onTouchEvent(event)) {
			return true;
		} else {
			return super.onTouchEvent(event);
		}
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return false;
	}
}

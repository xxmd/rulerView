package com.example.mylibrary.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.example.mylibrary.R;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class StraightRulerView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    @DrawableRes
    private int rulerBackground = -1;
    private Paint bigMarkPaint = new Paint();
    private float bigMarkLength = dpToPx(40f);
    private Paint smallMarkPaint = new Paint();
    private float smallMarkLength = dpToPx(20f);
    private TextPaint scaleTextPaint = new TextPaint();
    private float textMarkGap = dpToPx(10);
    private float scannedAreaWidth = 200;
    private Paint leftAreaPaint = new Paint();
    private Paint rightAreaPaint = new Paint();
    private float indicatorWidth = dpToPx(20);
    private float indicatorHeight = dpToPx(50);
    private Region indicatorRegion;
    private boolean startInIndicatorRegion = false;

    private Paint draggableLinePaint = new Paint();
    private TextPaint measureValuePaint = new TextPaint();
    private int canvasWidth;
    private float oneMMToPX;
    private float oneInchToPX;
    private TypedArray typedArray;
    private boolean showMeasureValue = true;
    private boolean showDraggableLine = true;
    private int indicatorColor;
    private int showMode;
    public static final int SHOW_BOTH = 0;
    public static final int HIDE_INCH= 1;
    public static final int ALL_CM = 2;

    public StraightRulerView(Context context) {
        super(context);
    }

    public StraightRulerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        typedArray = context.obtainStyledAttributes(attrs, R.styleable.StraightRulerView);
        init();
    }

    private void init() {
        initData();
        initPaint();
    }

    private void initData() {
        oneMMToPX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 1, getResources().getDisplayMetrics());
        oneInchToPX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_IN, 1, getResources().getDisplayMetrics());

        bigMarkLength = typedArray.getDimension(R.styleable.StraightRulerView_bigMarkLength, dpToPx(40f));
        smallMarkLength = typedArray.getDimension(R.styleable.StraightRulerView_smallMarkLength, dpToPx(20f));

        showDraggableLine = typedArray.getBoolean(R.styleable.StraightRulerView_showDraggableLine, true);
        showMeasureValue = typedArray.getBoolean(R.styleable.StraightRulerView_showMeasureValue, true);

        indicatorColor = typedArray.getColor(R.styleable.StraightRulerView_indicatorColor, Color.BLACK);
        indicatorWidth = typedArray.getDimension(R.styleable.StraightRulerView_indicatorWidth, dpToPx(20));
        indicatorHeight = typedArray.getDimension(R.styleable.StraightRulerView_indicatorHeight, dpToPx(50));

        showMode = typedArray.getInt(R.styleable.StraightRulerView_showMode, 0);
    }

    private void initPaint() {
        int markColor = typedArray.getColor(R.styleable.StraightRulerView_markColor, Color.BLACK);
        float bigMarkWidth = typedArray.getDimension(R.styleable.StraightRulerView_bigMarkWidth, dpToPx(2));
        bigMarkPaint.setColor(markColor);
        bigMarkPaint.setStyle(Paint.Style.FILL);
        bigMarkPaint.setStrokeWidth(bigMarkWidth);
        bigMarkPaint.setAntiAlias(true);

        float smallMarkWidth = typedArray.getDimension(R.styleable.StraightRulerView_smallMarkWidth, dpToPx(1));
        smallMarkPaint.setColor(markColor);
        smallMarkPaint.setStyle(Paint.Style.FILL);
        smallMarkPaint.setStrokeWidth(smallMarkWidth);
        smallMarkPaint.setAntiAlias(true);

        int scaleTextColor = typedArray.getColor(R.styleable.StraightRulerView_scaleTextColor, Color.BLACK);
        float scaleTextSize = typedArray.getDimension(R.styleable.StraightRulerView_scaleTextSize, 50f);
        scaleTextPaint.setColor(scaleTextColor);
        scaleTextPaint.setTextSize(scaleTextSize);
        scaleTextPaint.setAntiAlias(true);

        int leftAreaColor = typedArray.getColor(R.styleable.StraightRulerView_leftAreaColor, Color.parseColor("#daeaff"));
        leftAreaPaint.setColor(leftAreaColor);
        leftAreaPaint.setStyle(Paint.Style.FILL);

        int rightAreaColor = typedArray.getColor(R.styleable.StraightRulerView_rightAreaColor, Color.parseColor("#d7edea"));
        rightAreaPaint.setColor(rightAreaColor);
        rightAreaPaint.setStyle(Paint.Style.FILL);

        int measureValueColor = typedArray.getColor(R.styleable.StraightRulerView_measureValueColor, Color.BLACK);
        float measureValueTextSize = typedArray.getDimension(R.styleable.StraightRulerView_measureValueTextSize, 100f);
        measureValuePaint.setColor(measureValueColor);
        measureValuePaint.setTextSize(measureValueTextSize);
        measureValuePaint.setAntiAlias(true);

        int draggableLineColor = typedArray.getColor(R.styleable.StraightRulerView_draggableLineColor, Color.BLACK);
        float draggableLineWidth = typedArray.getDimension(R.styleable.StraightRulerView_draggableLineWidth, dpToPx(2));
        draggableLinePaint.setColor(draggableLineColor);
        draggableLinePaint.setStyle(Paint.Style.FILL);
        draggableLinePaint.setStrokeWidth(draggableLineWidth);
        draggableLinePaint.setAntiAlias(true);
    }

    private void drawMarkLayer(Canvas canvas) {
        // Avoid obstructing the scales at both ends
        canvas.scale(0.97f, 1, canvas.getWidth() / 2, 0);

        // draw cm mark
        drawMark(canvas, oneMMToPX, bigMarkPaint, smallMarkPaint, scaleTextPaint);
        canvas.save();
        canvas.rotate(180, canvas.getWidth() / 2, canvas.getHeight() / 2);

        switch (showMode) {
            case SHOW_BOTH:
                // draw inch mark
                drawMark(canvas, oneInchToPX / 10, bigMarkPaint, smallMarkPaint, scaleTextPaint);
                break;
            case HIDE_INCH:
                break;
            case ALL_CM:
                // repeat draw cm mark
                drawMark(canvas, oneMMToPX, bigMarkPaint, smallMarkPaint, scaleTextPaint);
                break;
        }
        canvas.restore();
    }

    private void drawMeasureValue(Canvas canvas) {
        drawCMValue(canvas);
        if (showMode == SHOW_BOTH) {
            drawInchValue(canvas);
        }
    }

    public float getCMValue() {
        return scannedAreaWidth / oneMMToPX / 10;
    }

    public float getInchValue() {
        return (canvasWidth - scannedAreaWidth) / oneInchToPX;
    }

    private void drawCMValue(Canvas canvas) {
        float cmValue = getCMValue();
        String cmValueFormat = formatCMValue(cmValue);
        Rect rect = new Rect();
        measureValuePaint.getTextBounds(cmValueFormat, 0, cmValueFormat.length(), rect);
        int y = 0;
        switch (showMode) {
            case SHOW_BOTH:
                y = (int) (canvas.getHeight() / 2 - textMarkGap / 2);
                break;
            case HIDE_INCH:
            case ALL_CM:
                y = canvas.getHeight() / 2 - rect.height() / 2;
                break;
        }
        if (scannedAreaWidth > rect.width() + textMarkGap) {
            canvas.drawText(cmValueFormat, scannedAreaWidth - rect.width() - textMarkGap, y, measureValuePaint);
        } else {
            canvas.drawText(cmValueFormat, scannedAreaWidth + textMarkGap, y, measureValuePaint);
        }
    }

    private void drawInchValue(Canvas canvas) {
        float inchValue = getInchValue();
        String inchValueFormat = formatInchValue(inchValue);
        Rect rect = new Rect();
        measureValuePaint.getTextBounds(inchValueFormat, 0, inchValueFormat.length(), rect);
        canvas.save();
        canvas.rotate(180, canvas.getWidth() / 2, canvas.getHeight() / 2);
        int y = (int) (canvas.getHeight() / 2 - textMarkGap / 2);
        float indicatorX = canvasWidth - scannedAreaWidth;
        if (indicatorX > rect.width() + textMarkGap) {
            canvas.drawText(inchValueFormat, indicatorX - rect.width() - textMarkGap, y, measureValuePaint);
        } else {
            canvas.drawText(inchValueFormat, indicatorX + textMarkGap, y, measureValuePaint);
        }
        canvas.restore();
    }

    private String formatCMValue(float cmValue) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        String format = decimalFormat.format(cmValue);
        return format + " cm";
    }

    private String formatInchValue(float inchValue) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        String format = decimalFormat.format(inchValue);
        return format + " inch";
    }


    private void drawDraggedArea(Canvas canvas) {
        Rect leftRect = new Rect(0, 0, (int) scannedAreaWidth, canvas.getHeight());
        canvas.drawRect(leftRect, leftAreaPaint);

        Rect rightRect = new Rect((int) scannedAreaWidth, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawRect(rightRect, rightAreaPaint);
    }

    private void drawDraggableLine(Canvas canvas) {
        canvas.drawLine(scannedAreaWidth, 0, scannedAreaWidth, canvas.getHeight(), draggableLinePaint);
    }

    private void drawMark(Canvas canvas, float oneUnitToPx, Paint bigMarkPaint, Paint smallMarkPaint, TextPaint scaleTextPaint) {
        for (int i = 0; i < canvas.getWidth() / oneUnitToPx; i++) {
            float startX = i * oneUnitToPx;
            if (i % 10 == 0) {
                drawBigMark(canvas, i / 10, startX, bigMarkPaint, scaleTextPaint);
            } else {
                drawSmallMark(canvas, startX, smallMarkPaint);
            }
        }
    }

    private void drawSmallMark(Canvas canvas, float startX, Paint smallMarkPaint) {
        canvas.drawLine(startX, 0f, startX, smallMarkLength, smallMarkPaint);
    }

    private void drawBigMark(Canvas canvas, int i, float startX, Paint bigMarkPaint, TextPaint scaleTextPaint) {
        canvas.drawLine(startX, 0f, startX, bigMarkLength, bigMarkPaint);
        String cmValue = String.valueOf(i);
        Rect rect = new Rect();
        scaleTextPaint.getTextBounds(cmValue, 0, cmValue.length(), rect);
        canvas.drawText(cmValue, startX - rect.width() / 2, bigMarkLength + textMarkGap + rect.height() / 2, scaleTextPaint);
    }

    public static float dpToPx(Context context, float dpValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.getResources().getDisplayMetrics());
    }

    public float dpToPx(float dpValue) {
        return dpToPx(getContext(), dpValue);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        draw();
    }

    public void draw() {
        Canvas canvas = getHolder().lockCanvas();
        canvasWidth = canvas.getWidth();
        drawBackground(canvas);
        drawDraggedArea(canvas);
        drawMarkLayer(canvas);
        if (showDraggableLine) {
            drawDraggableLine(canvas);
            drawIndicator(canvas);
        }
        if (showMeasureValue) {
            drawMeasureValue(canvas);
        }
        getHolder().unlockCanvasAndPost(canvas);
    }

    public void drawIndicator(Canvas canvas) {
        float left = scannedAreaWidth - indicatorWidth / 2;
        float top = canvas.getHeight() / 2 - indicatorHeight / 2;
        float right = scannedAreaWidth + indicatorWidth / 2;
        float bottom = canvas.getHeight() / 2 + indicatorHeight / 2;
        Rect rect = new Rect((int) left, (int) top, (int) right, (int) bottom);
        indicatorRegion = getIndicatorRegion(canvas);
        Paint paint = new Paint();
        paint.setColor(indicatorColor);
        canvas.drawRect(rect, paint);
    }

    /**
     * For the convenience of touch control, the indicator region is bigger than actual occupied region
     * @param canvas
     * @return
     */
    public Region getIndicatorRegion(Canvas canvas) {
        float left = scannedAreaWidth - indicatorWidth;
        float top = canvas.getHeight() / 2 - indicatorHeight;
        float right = scannedAreaWidth + indicatorWidth;
        float bottom = canvas.getHeight() / 2 + indicatorHeight;
        Rect rect = new Rect((int) left, (int) top, (int) right, (int) bottom);
        return new Region(rect);
    }

    private void drawBackground(Canvas canvas) {
        TypedValue backgroundValue = new TypedValue();
        typedArray.getValue(R.styleable.StraightRulerView_rulerBackground, backgroundValue);
        if (backgroundValue.type == TypedValue.TYPE_REFERENCE) {
            int resourceId = typedArray.getResourceId(R.styleable.StraightRulerView_rulerBackground, Integer.MIN_VALUE);
            if (resourceId != Integer.MIN_VALUE) {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resourceId);
                canvas.drawBitmap(bitmap, 0, 0, null);
            }
        } else {
            int color = typedArray.getColor(R.styleable.StraightRulerView_rulerBackground, Color.WHITE);
            canvas.drawColor(color);
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

    }

    @Override
    public void run() {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startInIndicatorRegion = indicatorRegion.contains((int) event.getX(), (int) event.getY());
                Log.e("ACTION_DOWN", String.valueOf(startInIndicatorRegion));
                break;
            case MotionEvent.ACTION_MOVE:
                if (startInIndicatorRegion) {
                    scannedAreaWidth = event.getX();
                    if (scannedAreaWidth > canvasWidth) {
                        scannedAreaWidth = canvasWidth;
                    }
                }
                draw();
                break;
            case MotionEvent.ACTION_UP:
                startInIndicatorRegion = false;
                break;
        }
        return true;
    }
}

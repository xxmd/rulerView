package com.example.mylibrary.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class StraightRulerView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    @DrawableRes
    private int rulerBackground = -1;
    private Paint bigMarkPaint = new Paint();
    private float bigMarkLength = dpToPx(40f);
    private Paint smallMarkPaint = new Paint();
    private float smallMarkLength = dpToPx(20f);
    private TextPaint textPaint = new TextPaint();
    private float textMarkGap = dpToPx(10);
    private float scannedAreaWidth = 200;
    private Paint leftAreaPaint = new Paint();
    private Paint rightAreaPaint = new Paint();
    private float indicatorWidth = dpToPx(20);
    private float indicatorHeight = dpToPx(50);
    private Region indicatorRegion;
    private boolean startInIndicatorRegion = false;

    private TextPaint measureValuePaint = new TextPaint();
    private int canvasWidth;
    private float oneMMToPX;
    private float oneInchToPX;

    public StraightRulerView(Context context) {
        super(context);
    }

    public StraightRulerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        init();
    }
    private void init() {
        initPaint();
    }

    private void initPaint() {
        bigMarkPaint.setColor(Color.BLACK);
        bigMarkPaint.setStyle(Paint.Style.FILL);
        bigMarkPaint.setStrokeWidth(dpToPx(2));
        bigMarkPaint.setAntiAlias(true);

        smallMarkPaint.setColor(Color.BLACK);
        smallMarkPaint.setStyle(Paint.Style.FILL);
        smallMarkPaint.setStrokeWidth(dpToPx(1));
        smallMarkPaint.setAntiAlias(true);

        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(50);
        textPaint.setAntiAlias(true);

        leftAreaPaint.setColor(Color.parseColor("#daeaff"));
        leftAreaPaint.setStyle(Paint.Style.FILL);

        rightAreaPaint.setColor(Color.parseColor("#d7edea"));
        rightAreaPaint.setStyle(Paint.Style.FILL);

        measureValuePaint.setColor(Color.BLACK);
        measureValuePaint.setTextSize(100);
        measureValuePaint.setAntiAlias(true);
    }

    private void drawMarkLayer(Canvas canvas) {
        // Avoid obstructing the scales at both ends
        canvas.scale(0.97f, 1, canvas.getWidth() / 2, 0);
//        if (rulerBackground > 0) {
//
//        }
        canvas.drawColor(Color.WHITE);
        drawDraggedArea(canvas);

        oneMMToPX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 1, getResources().getDisplayMetrics());
        drawMark(canvas, oneMMToPX, bigMarkPaint, smallMarkPaint, textPaint);
        canvas.save();
        canvas.rotate(180, canvas.getWidth() / 2, canvas.getHeight()/ 2);
        oneInchToPX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_IN, 1, getResources().getDisplayMetrics());
        drawMark(canvas, oneInchToPX / 10, bigMarkPaint, smallMarkPaint, textPaint);
        canvas.restore();
        drawDraggableLine(canvas);
        drawMeasureValue(canvas);
    }

    private void drawMeasureValue(Canvas canvas) {
        drawCMValue(canvas);
        drawInchValue(canvas);
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
        int y = canvas.getHeight() / 4 + rect.height() / 2;
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
        int y = canvas.getHeight() / 4 + rect.height() / 2;
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
        canvas.drawLine(scannedAreaWidth, 0, scannedAreaWidth, canvas.getHeight(), bigMarkPaint);
        float left = scannedAreaWidth - indicatorWidth / 2;
        float top = canvas.getHeight() / 2 - indicatorHeight / 2;
        float right = scannedAreaWidth + indicatorWidth / 2;
        float bottom = canvas.getHeight() / 2 + indicatorHeight / 2;
        Rect rect = new Rect((int) left, (int) top, (int) right, (int) bottom);
        indicatorRegion = new Region(rect);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawRect(rect, paint);
    }

    private void drawMark(Canvas canvas, float oneUnitToPx, Paint bigMarkPaint, Paint smallMarkPaint, TextPaint textPaint) {
        for (int i = 0; i < canvas.getWidth() / oneUnitToPx; i++) {
            float startX = i * oneUnitToPx;
            if (i % 10 == 0) {
                drawBigMark(canvas, i / 10, startX, bigMarkPaint, textPaint);
            } else {
                drawSmallMark(canvas, startX, smallMarkPaint);
            }
        }
    }

    private void drawSmallMark(Canvas canvas, float startX, Paint smallMarkPaint) {
        canvas.drawLine(startX,  0f, startX, smallMarkLength, smallMarkPaint);
    }

    private void drawBigMark(Canvas canvas, int i, float startX, Paint bigMarkPaint, TextPaint textPaint) {
        canvas.drawLine(startX,  0f, startX, bigMarkLength, bigMarkPaint);
        String cmValue = String.valueOf(i);
        Rect rect = new Rect();
        textPaint.getTextBounds(cmValue, 0, cmValue.length(), rect);
        canvas.drawText(cmValue, startX - rect.width() / 2, bigMarkLength + textMarkGap + rect.height() / 2, textPaint);
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
        drawDraggedArea(canvas);
        drawMarkLayer(canvas);
        drawDraggableLine(canvas);
        getHolder().unlockCanvasAndPost(canvas);
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
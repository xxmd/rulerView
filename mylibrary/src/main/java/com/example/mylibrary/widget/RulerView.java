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
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public class RulerView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    @ColorInt
    private int rulerBackground = Color.WHITE;
    private Paint bigMarkPaint = new Paint();
    private float bigMarkLength = dpToPx(40f);
    private Paint smallMarkPaint = new Paint();
    private float smallMarkLength = dpToPx(20f);
    private TextPaint textPaint = new TextPaint();
    private float textMarkGap = dpToPx(10);
    private float scannedAreaWidth = 200;
    private Paint scannedAreaPaint = new Paint();
    private float indicatorWidth = dpToPx(20);
    private float indicatorHeight = dpToPx(50);
    private Region indicatorRegion;
    private boolean startInIndicatorRegion = false;
    private int canvasWidth;

    public RulerView(Context context) {
        super(context);
    }

    public RulerView(Context context, AttributeSet attrs) {
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

        scannedAreaPaint.setColor(Color.parseColor("#daeaff"));
        scannedAreaPaint.setStyle(Paint.Style.FILL);
    }

    private void initView() {
//        drawMarkLayer();
    }

    private void drawMarkLayer(Canvas canvas) {
        // Avoid obstructing the scales at both ends
        canvas.scale(0.97f, 1, canvas.getWidth() / 2, 0);
        canvas.drawColor(rulerBackground);
        drawScannedArea(canvas);

        float oneMMToPX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 1, getResources().getDisplayMetrics());
        drawMark(canvas, oneMMToPX, bigMarkPaint, smallMarkPaint, textPaint);
        canvas.save();
        canvas.rotate(180, canvas.getWidth() / 2, canvas.getHeight()/ 2);
        float oneInchToPX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_IN, 1, getResources().getDisplayMetrics());
        drawMark(canvas, oneInchToPX / 10, bigMarkPaint, smallMarkPaint, textPaint);
        canvas.restore();
        drawDraggableLine(canvas);
    }

    private void drawScannedArea(Canvas canvas) {
        Rect rect = new Rect(0, 0, (int) scannedAreaWidth, canvas.getHeight());
        canvas.drawRect(rect, scannedAreaPaint);
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
        drawScannedArea(canvas);
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

package com.example.drawingapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DrawView extends View {

    private Context context;
    private float x, y; // X轴起点, Y轴起点
    private final Paint paint = new Paint();
    private final Path path = new Path();
    private Canvas canvas;
    private Bitmap bitmap;
    private int paintWidth = 30; // 画笔的宽度
    private int paintColor = Color.WHITE; // 画笔颜色
    private int backgroundColor = Color.BLACK; // 背景颜色
    private boolean isTouched = false; // 是否已经签名

    float minX = -1, minY = -1, maxX = -1, maxY = -1; // 用于记录有效绘制区域的边界

    public interface Touch {
        void OnTouch(boolean isTouch);
    }

    private Touch touch;

    public DrawView(Context context) {
        super(context);
        init(context);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DrawView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        paint.setAntiAlias(true); // 抗锯齿
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND); // 线冒
        paint.setColor(paintColor); // 画笔颜色
        paint.setStrokeWidth(paintWidth); // 画笔宽度
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setHasAlpha(true);
        canvas = new Canvas(bitmap);
        isTouched = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (touch != null) touch.OnTouch(true);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (touch != null) touch.OnTouch(false);
                touchMove(event);
                break;
            case MotionEvent.ACTION_UP:
                isTouched = true;
                canvas.drawPath(path, paint);
                path.reset();
                canvas.drawPoint(event.getX(), event.getY(), paint);
                checkEffectivePoint(event.getX(), event.getY());
                break;
        }
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        canvas.drawPath(path, paint);
    }

    private void touchDown(MotionEvent event) {
        path.reset();
        float downX = event.getX();
        float downY = event.getY();
        x = downX;
        y = downY;
        path.moveTo(downX, downY);
    }

    private void touchMove(MotionEvent event) {
        final float moveX = event.getX();
        final float moveY = event.getY();
        final float previousX = x;
        final float previousY = y;
        final float dx = Math.abs(moveX - previousX);
        final float dy = Math.abs(moveY - previousY);
        if (dx >= 3 || dy >= 3) {
            float cX = (moveX + previousX) / 2;
            float cY = (moveY + previousY) / 2;
            path.quadTo(previousX, previousY, cX, cY);
            x = moveX;
            y = moveY;
        }
        checkEffectivePoint(x, y);
    }

    public void checkEffectivePoint(float x, float y) {
        int mCanvasWidth = canvas.getWidth();
        int mCanvasHeight = canvas.getHeight();
        updateMinAndMax(x, y, mCanvasWidth, mCanvasHeight);
    }

    private void updateMinAndMax(float x, float y, int mCanvasWidth, int mCanvasHeight) {
        if (x >= 0 && x <= mCanvasWidth) {
            minX = updateMin(x - paintWidth, minX, 0);
            maxX = updateMax(x + paintWidth, maxX, mCanvasWidth);
        }
        if (y >= 0 && y <= mCanvasHeight) {
            minY = updateMin(y - paintWidth, minY, 0);
            maxY = updateMax(y + paintWidth, maxY, mCanvasHeight);
        }
    }

    private float updateMin(float current, float min, float limit) {
        if (current < min || min == -1) {
            return Math.max(current, limit);
        }
        return min;
    }

    private float updateMax(float current, float max, float limit) {
        if (current > max || max == -1) {
            return Math.min(current, limit);
        }
        return max;
    }

    public void clear() {
        if (canvas != null) {
            isTouched = false;
            paint.setColor(paintColor);
            paint.setStrokeWidth(paintWidth);
            canvas.drawColor(backgroundColor, PorterDuff.Mode.CLEAR);
            invalidate();
        }
    }

    public Bitmap getBitmap() {
        if (!isTouched) {
            return null;
        }
        setDrawingCacheEnabled(true);
        Bitmap cacheBitmap = Bitmap.createBitmap(getDrawingCache());
        setDrawingCacheEnabled(false);
        return cacheBitmap;
    }
}
package com.example.wheelcontroller.classes;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.wheelcontroller.R;

public class MyProgressBar extends View {
    private float STROKE_WIDTH = 12f, angle;
    private long duration = 2000;
    private final Paint paintBrush = new Paint();

    private final int[] COLORS = {Color.RED,Color.GREEN,Color.BLUE};
    private final int COLOR_SIZE = COLORS.length;
    private int colorIndex = 0;

    private ProgressListener progressListener = null;
    private boolean runLoop = false;
    private boolean autoRun = true;
    private float swipeAngle = 20f;
    private boolean forcedStop = false;

    public MyProgressBar(Context context) {
        super(context);
    }

    public MyProgressBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.AutoPainter,
                0, 0);
        try {
            STROKE_WIDTH = a.getDimension(R.styleable.AutoPainter_pathWidth,10);
            angle = a.getFloat(R.styleable.AutoPainter_angle,0);
            duration = a.getInt(R.styleable.AutoPainter_duration,2000);
            runLoop = a.getBoolean(R.styleable.AutoPainter_loop,false);
            autoRun = a.getBoolean(R.styleable.AutoPainter_auto_run,true);
        } finally {
            a.recycle();
        }

        if(!autoRun){
            swipeAngle = 0f;
        }
        initializeBrushes();

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(forcedStop) return;

        if(runLoop){
            canvas.drawArc( STROKE_WIDTH,0+STROKE_WIDTH,
                    getWidth()-STROKE_WIDTH, getHeight()-STROKE_WIDTH,
                    angle,angle-swipeAngle,false,paintBrush);
        }
        else {
            canvas.drawArc(STROKE_WIDTH, 0 + STROKE_WIDTH,
                    getWidth() - STROKE_WIDTH, getHeight() - STROKE_WIDTH,
                    0, -angle, false, paintBrush);
        }
    }

    private void initializeBrushes(){
        paintBrush.setAntiAlias(true);
        paintBrush.setStyle(Paint.Style.STROKE);
        paintBrush.setStrokeCap(Paint.Cap.ROUND);
        paintBrush.setStrokeJoin(Paint.Join.ROUND);
        paintBrush.setColor(Color.GREEN);
        paintBrush.setStrokeWidth(STROKE_WIDTH);

        paintBrush.setShader(
                new LinearGradient(
                        0,0, 20f,20f,
                        Color.BLACK,
                        Color.GRAY,
                        Shader.TileMode.REPEAT
                )
        );

        if(autoRun) {
            startAnimation();
        }
    }

    private void setShader(int index){
        paintBrush.setShader(
                new LinearGradient(
                        0,0, 20f,20f,
                        COLORS[index],
                        COLORS[index],
                        Shader.TileMode.REPEAT
                )
        );
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public interface ProgressListener{
        void onAnimationEnd();
    }

    public void startProgress(){
        forcedStop = false;
        startAnimation();
    }

    public void hideView(){
        forcedStop = true;
    }

    public void resetProgress(){
        swipeAngle = 360f;
        setShader(2);
        invalidate();
        runLoop = false;
        //forcedStop = true;
    }

    private void startAnimation(){
        ValueAnimator animator = ValueAnimator.ofFloat(0,-360);
        animator.setDuration(duration);
        animator.addUpdateListener(animation -> {
            angle = (float) animation.getAnimatedValue();
            invalidate();
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if(progressListener != null) progressListener.onAnimationEnd();
                if(runLoop){
                    angle = 0;
                    setShader(colorIndex);
                    colorIndex = (colorIndex+1) % COLOR_SIZE;
                    startAnimation();
                }
            }
        });
        animator.start();

    }

}

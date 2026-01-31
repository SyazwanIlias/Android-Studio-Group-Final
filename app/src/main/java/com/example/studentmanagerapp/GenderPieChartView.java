package com.example.studentmanagerapp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class GenderPieChartView extends View {

    private Paint malePaint;
    private Paint femalePaint;
    private Paint backgroundPaint;
    private Paint centerPaint;

    private int maleCount = 0;
    private int femaleCount = 0;
    private float animatedAngle = 0f;
    private float targetAngle = 0f;

    private RectF arcRect;
    private float strokeWidth;

    // Donut hole size (0.0 to 1.0, where 0.5 means hole is half the radius)
    private static final float HOLE_RADIUS_RATIO = 0.6f;

    public GenderPieChartView(Context context) {
        super(context);
        init();
    }

    public GenderPieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GenderPieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Male paint (Blue)
        malePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        malePaint.setColor(0xFF42A5F5); // chart_blue
        malePaint.setStyle(Paint.Style.FILL);

        // Female paint (Pink)
        femalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        femalePaint.setColor(0xFFEC407A); // chart_pink
        femalePaint.setStyle(Paint.Style.FILL);

        // Background circle paint (light gray)
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(0xFFE8E8E8);
        backgroundPaint.setStyle(Paint.Style.FILL);

        // Center hole paint (white)
        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setColor(0xFFFFFFFF);
        centerPaint.setStyle(Paint.Style.FILL);

        arcRect = new RectF();
    }

    public void setGenderCounts(int male, int female) {
        this.maleCount = male;
        this.femaleCount = female;

        int total = male + female;
        if (total > 0) {
            targetAngle = (male / (float) total) * 360f;
        } else {
            targetAngle = 0f;
        }

        // Animate from 0 to target angle
        animateChart();
    }

    private void animateChart() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, targetAngle);
        animator.setDuration(1200);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animatedAngle = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height);

        float centerX = width / 2f;
        float centerY = height / 2f;
        float radius = size / 2f - 20;
        float holeRadius = radius * HOLE_RADIUS_RATIO;

        // Draw background circle
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint);

        int total = maleCount + femaleCount;
        if (total > 0) {
            // Setup arc rectangle
            arcRect.set(centerX - radius, centerY - radius,
                    centerX + radius, centerY + radius);

            // Draw male arc (blue)
            if (animatedAngle > 0) {
                canvas.drawArc(arcRect, -90, animatedAngle, true, malePaint);
            }

            // Draw female arc (pink) - only if animation has progressed
            if (animatedAngle >= targetAngle && femaleCount > 0) {
                float femaleAngle = 360f - targetAngle;
                canvas.drawArc(arcRect, -90 + targetAngle, femaleAngle, true, femalePaint);
            }
        }

        // Draw center hole to create donut effect
        canvas.drawCircle(centerX, centerY, holeRadius, centerPaint);

        // Optional: Draw subtle shadow for depth
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(0x20000000);
        canvas.drawCircle(centerX, centerY + 2, holeRadius - 4, shadowPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(width, height);

        setMeasuredDimension(size, size);
    }
}
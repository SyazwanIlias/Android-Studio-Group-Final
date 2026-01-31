package com.example.studentmanagerapp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class BirthdayBarChartView extends View {

    private Paint barPaint;
    private Paint barBackgroundPaint;
    private Paint textPaint;
    private Paint labelPaint;

    private int[] data = new int[12];
    private float[] animatedHeights = new float[12];
    private int maxValue = 1;

    private String[] monthLabels = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private static final int BAR_COLOR = 0xFF7CB342; // primary color
    private static final int BAR_GRADIENT_COLOR = 0xFF9CCC65; // primary_light
    private static final int BACKGROUND_BAR_COLOR = 0xFFE8E8E8;
    private static final int TEXT_COLOR = 0xFF212121;
    private static final int LABEL_COLOR = 0xFF757575;

    public BirthdayBarChartView(Context context) {
        super(context);
        init();
    }

    public BirthdayBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BirthdayBarChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Bar paint with gradient
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setColor(BAR_COLOR);

        // Background bar paint
        barBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barBackgroundPaint.setStyle(Paint.Style.FILL);
        barBackgroundPaint.setColor(BACKGROUND_BAR_COLOR);

        // Text paint for values
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(TEXT_COLOR);
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        // Label paint for month names
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(LABEL_COLOR);
        labelPaint.setTextSize(24f);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        // Initialize animated heights to 0
        for (int i = 0; i < 12; i++) {
            animatedHeights[i] = 0f;
        }
    }

    public void setData(int[] data) {
        if (data.length == 12) {
            this.data = data;

            // Find max value
            maxValue = 1;
            for (int value : data) {
                if (value > maxValue) {
                    maxValue = value;
                }
            }

            // Animate bars
            animateBars();
        }
    }

    private void animateBars() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1500);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            for (int i = 0; i < 12; i++) {
                animatedHeights[i] = data[i] * progress;
            }
            invalidate();
        });
        animator.setStartDelay(300); // Start after chart appears
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) return;

        // Padding
        float paddingTop = 60f;
        float paddingBottom = 80f;
        float paddingLeft = 40f;
        float paddingRight = 40f;

        float chartHeight = height - paddingTop - paddingBottom;
        float chartWidth = width - paddingLeft - paddingRight;

        // Bar width calculation (thicker bars with spacing)
        float barSpacing = 8f;
        float barWidth = (chartWidth - (11 * barSpacing)) / 12f;

        // Make sure bars aren't too thin
        barWidth = Math.max(barWidth, 30f);

        for (int i = 0; i < 12; i++) {
            float x = paddingLeft + i * (barWidth + barSpacing);

            // Calculate bar height based on value
            float barHeight = maxValue > 0 ?
                    (animatedHeights[i] / maxValue) * chartHeight : 0;

            float barTop = paddingTop + chartHeight - barHeight;
            float barBottom = paddingTop + chartHeight;

            // Draw background bar (full height, light)
            RectF backgroundRect = new RectF(
                    x,
                    paddingTop,
                    x + barWidth,
                    barBottom
            );
            canvas.drawRoundRect(backgroundRect, 12f, 12f, barBackgroundPaint);

            // Draw actual bar with gradient
            if (barHeight > 0) {
                RectF barRect = new RectF(
                        x,
                        barTop,
                        x + barWidth,
                        barBottom
                );

                // Create gradient for bar
                LinearGradient gradient = new LinearGradient(
                        x, barTop,
                        x, barBottom,
                        BAR_GRADIENT_COLOR,
                        BAR_COLOR,
                        Shader.TileMode.CLAMP
                );
                barPaint.setShader(gradient);

                canvas.drawRoundRect(barRect, 12f, 12f, barPaint);

                // Reset shader
                barPaint.setShader(null);

                // Draw value on top of bar if there's space
                if (data[i] > 0 && barHeight > 40) {
                    canvas.drawText(
                            String.valueOf(data[i]),
                            x + barWidth / 2,
                            barTop - 10,
                            textPaint
                    );
                }
            }

            // Draw month label below
            canvas.drawText(
                    monthLabels[i],
                    x + barWidth / 2,
                    barBottom + 35,
                    labelPaint
            );
        }

        // Draw a subtle baseline
        Paint baselinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        baselinePaint.setColor(0xFFBDBDBD);
        baselinePaint.setStrokeWidth(2f);
        canvas.drawLine(
                paddingLeft,
                paddingTop + chartHeight,
                width - paddingRight,
                paddingTop + chartHeight,
                baselinePaint
        );
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        // Ensure minimum height for proper display
        height = Math.max(height, 400);

        setMeasuredDimension(width, height);
    }
}
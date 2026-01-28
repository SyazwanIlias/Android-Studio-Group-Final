package com.example.studentmanagerapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class BirthdayBarChartView extends View {

    private int[] monthlyCounts = new int[12];
    private Paint barPaint;
    private Paint textPaint;
    private Paint axisPaint;
    private String[] monthLabels = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    public BirthdayBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint = new Paint();
        barPaint.setColor(Color.parseColor("#CCFF00")); // Neon Yellow/Green
        barPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.parseColor("#71767D")); // Dim text
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        axisPaint = new Paint();
        axisPaint.setColor(Color.parseColor("#1C1F26"));
        axisPaint.setStrokeWidth(2);
    }

    public void setData(int[] counts) {
        this.monthlyCounts = counts;
        invalidate(); // Redraw view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int padding = 60;
        int bottomAxis = height - 80;
        int leftAxis = 80;

        // Draw Axes
        canvas.drawLine(leftAxis, padding, leftAxis, bottomAxis, axisPaint); // Y
        canvas.drawLine(leftAxis, bottomAxis, width - padding, bottomAxis, axisPaint); // X

        int maxCount = 1;
        for (int count : monthlyCounts) {
            if (count > maxCount) maxCount = count;
        }

        float chartWidth = width - leftAxis - padding;
        float barWidth = (chartWidth / 12) * 0.7f;
        float spacing = (chartWidth / 12) * 0.3f;

        for (int i = 0; i < 12; i++) {
            float left = leftAxis + spacing + (i * (barWidth + spacing));
            float barHeight = ((float) monthlyCounts[i] / maxCount) * (bottomAxis - padding - 40);
            float top = bottomAxis - barHeight;
            float right = left + barWidth;

            // Draw Bar
            canvas.drawRect(left, top, right, bottomAxis, barPaint);

            // Draw Month Label
            canvas.drawText(monthLabels[i], left + (barWidth / 2), bottomAxis + 40, textPaint);

            // Draw Count on top of bar
            if (monthlyCounts[i] > 0) {
                canvas.drawText(String.valueOf(monthlyCounts[i]), left + (barWidth / 2), top - 10, textPaint);
            }
        }
    }
}

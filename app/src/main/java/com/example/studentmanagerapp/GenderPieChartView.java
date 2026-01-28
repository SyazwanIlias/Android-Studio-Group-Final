package com.example.studentmanagerapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class GenderPieChartView extends View {
    private Paint malePaint, femalePaint, emptyPaint;
    private RectF rectF;
    private float maleAngle = 0, femaleAngle = 0;

    public GenderPieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Setup Male Paint (Blue)
        malePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        malePaint.setColor(Color.parseColor("#2196F3"));
        malePaint.setStyle(Paint.Style.FILL);

        // Setup Female Paint (Pink)
        femalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        femalePaint.setColor(Color.parseColor("#E91E63"));
        femalePaint.setStyle(Paint.Style.FILL);

        // Setup Empty Paint (Gray)
        emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emptyPaint.setColor(Color.LTGRAY);
        emptyPaint.setStyle(Paint.Style.FILL);

        rectF = new RectF();
    }

    /**
     * UPDATED: Method name matches your ReportsActivity call
     */
    public void setGenderCounts(int male, int female) {
        int total = male + female;
        if (total == 0) {
            maleAngle = 0;
            femaleAngle = 0;
        } else {
            // Calculate slice sizes
            maleAngle = (360f * male) / total;
            femaleAngle = (360f * female) / total;
        }
        // Force the chart to redraw
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Center the pie chart in the available space
        float width = getWidth();
        float height = getHeight();
        float size = Math.min(width, height) - 80; // Margin for breathing room

        float left = (width - size) / 2;
        float top = (height - size) / 2;

        rectF.set(left, top, left + size, top + size);

        if (maleAngle == 0 && femaleAngle == 0) {
            // Draw empty circle if no buddies added yet
            canvas.drawOval(rectF, emptyPaint);
        } else {
            // Draw slices starting from the top (-90 degrees)
            canvas.drawArc(rectF, -90, maleAngle, true, malePaint);
            canvas.drawArc(rectF, -90 + maleAngle, femaleAngle, true, femalePaint);
        }
    }
}
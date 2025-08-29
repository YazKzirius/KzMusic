package com.example.kzmusic;
// File: VisualizerView.java
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class VisualizerView extends View {
    private float[] magnitudes;
    private Paint barPaint = new Paint();
    private static final int BAR_COUNT = 60;
    private float[] smoothedMagnitudes;

    public VisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        barPaint.setColor(Color.WHITE);
        barPaint.setAntiAlias(true);
        smoothedMagnitudes = new float[BAR_COUNT];
    }

    public void updateVisualizer(float[] freshMagnitudes) {
        if (freshMagnitudes == null) return;
        this.magnitudes = freshMagnitudes;
        invalidate();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (magnitudes == null || getWidth() == 0) return;

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float baseRadius = getWidth() * 0.28f;
        // Make the maximum bar length shorter for a more subtle effect.
        float maxBarLength = getWidth() * 0.15f; // Previously 0.18f

        barPaint.setStrokeWidth(getWidth() / 120f);

        for (int i = 0; i < BAR_COUNT; i++) {
            float angle = (float) (i * 360.0 / BAR_COUNT) - 90;

            // The rest of the logic remains the same.
            int magnitudeIndex = (int) (Math.pow((i + 1) / (float)BAR_COUNT, 2) * (magnitudes.length - 1));
            float magnitude = Math.min(magnitudes[magnitudeIndex] * 3.0f, 1.0f);

            smoothedMagnitudes[i] += (magnitude - smoothedMagnitudes[i]) * 0.4f;
            float barLength = smoothedMagnitudes[i] * maxBarLength;

            float startX = (float) (centerX + baseRadius * Math.cos(Math.toRadians(angle)));
            float startY = (float) (centerY + baseRadius * Math.sin(Math.toRadians(angle)));
            float endX = (float) (centerX + (baseRadius + barLength) * Math.cos(Math.toRadians(angle)));
            float endY = (float) (centerY + (baseRadius + barLength) * Math.sin(Math.toRadians(angle)));

            canvas.drawLine(startX, startY, endX, endY, barPaint);
        }
    }
}
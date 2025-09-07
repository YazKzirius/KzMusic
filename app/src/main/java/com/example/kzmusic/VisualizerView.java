// File: VisualizerView.java
package com.example.kzmusic;

// File: VisualizerView.java
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.Random;

public class VisualizerView extends View {
    public enum VisualizerMode {
        FOLDED, MIRRORED, PULSE, JUMBLED
    }
    private VisualizerMode currentMode = VisualizerMode.FOLDED;

    private float[] magnitudes;
    private Paint barPaint = new Paint();
    private static final int BAR_COUNT = 60;
    private float[] smoothedMagnitudes;
    private Random random = new Random();
    Boolean isRandomOn = false;

    public VisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        barPaint.setColor(Color.WHITE);
        barPaint.setAntiAlias(true);
        barPaint.setStrokeCap(Paint.Cap.BUTT);
        smoothedMagnitudes = new float[BAR_COUNT];
    }

    public void setRandomOn(boolean on) {
        this.isRandomOn = on;
    }

    public void setVisualizerMode(VisualizerMode mode) {
        this.currentMode = mode;
    }

    public void updateVisualizer(float[] freshMagnitudes) {
        if (freshMagnitudes == null) return;
        this.magnitudes = freshMagnitudes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (magnitudes == null || getWidth() == 0 || magnitudes.length < 2) return;

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float baseRadius = getWidth() * 0.28f;
        float maxBarLength = getWidth() * 0.15f;
        barPaint.setStrokeWidth(getWidth() / 120f);

        switch (currentMode) {
            case FOLDED:
                drawFoldedSpectrum(canvas, centerX, centerY, baseRadius, maxBarLength);
                break;
            case MIRRORED:
                drawMirroredSpectrum(canvas, centerX, centerY, baseRadius, maxBarLength);
                break;
            case PULSE:
                drawPulse(canvas, centerX, centerY, baseRadius, maxBarLength);
                break;
            case JUMBLED:
                drawJumbled(canvas, centerX, centerY, baseRadius, maxBarLength);
                break;
        }
    }
    private void drawFoldedSpectrum(Canvas canvas, float centerX, float centerY, float baseRadius, float maxBarLength) {
        for (int i = 0; i < BAR_COUNT; i++) { // Correct loop condition
            float angle = (float) (i * 360.0 / BAR_COUNT) - 90;
            int magnitudeIndex;
            if (i < BAR_COUNT / 2) {
                magnitudeIndex = i;
            } else {
                magnitudeIndex = BAR_COUNT - i;
            }
            magnitudeIndex = (int) ((magnitudeIndex / (float) (BAR_COUNT / 2)) * (magnitudes.length - 1));
            if (magnitudeIndex >= magnitudes.length) magnitudeIndex = magnitudes.length - 1;

            float magnitude = Math.min(magnitudes[magnitudeIndex] * 3.0f, 1.0f);
            smoothedMagnitudes[i] += (magnitude - smoothedMagnitudes[i]) * 0.4f;
            float barLength = smoothedMagnitudes[i] * maxBarLength;

            if (barLength < 2f) barLength = 2f;

            float startX = (float) (centerX + baseRadius * Math.cos(Math.toRadians(angle)));
            float startY = (float) (centerY + baseRadius * Math.sin(Math.toRadians(angle)));
            float endX = (float) (centerX + (baseRadius + barLength) * Math.cos(Math.toRadians(angle)));
            float endY = (float) (centerY + (baseRadius + barLength) * Math.sin(Math.toRadians(angle)));
            canvas.drawLine(startX, startY, endX, endY, barPaint);
        }
    }
    private void drawMirroredSpectrum(Canvas canvas, float centerX, float centerY, float baseRadius, float maxBarLength) {
        int halfBarCount = BAR_COUNT / 2;
        for (int i = 0; i < halfBarCount; i++) {
            int magnitudeIndex = (int) (Math.pow(i / (float) halfBarCount, 2) * (magnitudes.length - 1));
            if (magnitudeIndex >= magnitudes.length) magnitudeIndex = magnitudes.length - 1;

            float magnitude = Math.min(magnitudes[magnitudeIndex] * 3.0f, 1.0f);
            smoothedMagnitudes[i] += (magnitude - smoothedMagnitudes[i]) * 0.4f;
            float barLength = smoothedMagnitudes[i] * maxBarLength;

            if (barLength < 2f) barLength = 2f;

            // Draw Left Bar
            float angleLeft = -90 - (float)(i * 180.0 / halfBarCount);
            float startX = (float) (centerX + baseRadius * Math.cos(Math.toRadians(angleLeft)));
            float startY = (float) (centerY + baseRadius * Math.sin(Math.toRadians(angleLeft)));
            float endX = (float) (centerX + (baseRadius + barLength) * Math.cos(Math.toRadians(angleLeft)));
            float endY = (float) (centerY + (baseRadius + barLength) * Math.sin(Math.toRadians(angleLeft)));
            canvas.drawLine(startX, startY, endX, endY, barPaint);

            // Draw Right Bar (Mirrored)
            float angleRight = -90 + (float)(i * 180.0 / halfBarCount);
            float mirroredStartX = (float) (centerX + baseRadius * Math.cos(Math.toRadians(angleRight)));
            float mirroredStartY = (float) (centerY + baseRadius * Math.sin(Math.toRadians(angleRight)));
            float mirroredEndX = (float) (centerX + (baseRadius + barLength) * Math.cos(Math.toRadians(angleRight)));
            float mirroredEndY = (float) (centerY + (baseRadius + barLength) * Math.sin(Math.toRadians(angleRight)));
            canvas.drawLine(mirroredStartX, mirroredStartY, mirroredEndX, mirroredEndY, barPaint);
        }
    }

    private void drawPulse(Canvas canvas, float centerX, float centerY, float baseRadius, float maxBarLength) {
        float bassMagnitude = 0;
        int bassBins = Math.min(8, magnitudes.length);
        for (int i = 0; i < bassBins; i++) {
            bassMagnitude += magnitudes[i];
        }
        bassMagnitude /= bassBins;
        float magnitude = Math.min(bassMagnitude * 5.0f, 1.0f);
        smoothedMagnitudes[0] += (magnitude - smoothedMagnitudes[0]) * 0.4f;
        float barLength = smoothedMagnitudes[0] * maxBarLength;

        if (barLength < 2f) barLength = 2f;

        for (int i = 0; i < BAR_COUNT; i++) {
            float angle = (float) (i * 360.0 / BAR_COUNT) - 90;
            float startX = (float) (centerX + baseRadius * Math.cos(Math.toRadians(angle)));
            float startY = (float) (centerY + baseRadius * Math.sin(Math.toRadians(angle)));
            float endX = (float) (centerX + (baseRadius + barLength) * Math.cos(Math.toRadians(angle)));
            float endY = (float) (centerY + (baseRadius + barLength) * Math.sin(Math.toRadians(angle)));
            canvas.drawLine(startX, startY, endX, endY, barPaint);
        }
    }

    private void drawJumbled(Canvas canvas, float centerX, float centerY, float baseRadius, float maxBarLength) {
        for (int i = 0; i < BAR_COUNT; i++) {
            float angle = (float) (i * 360.0 / BAR_COUNT) - 90;
            int magnitudeIndex = random.nextInt(magnitudes.length);
            float magnitude = Math.min(magnitudes[magnitudeIndex] * 3.0f, 1.0f);
            smoothedMagnitudes[i] += (magnitude - smoothedMagnitudes[i]) * 0.4f;
            float barLength = smoothedMagnitudes[i] * maxBarLength;
            if (barLength < 2f) barLength = 2f;

            float startX = (float) (centerX + baseRadius * Math.cos(Math.toRadians(angle)));
            float startY = (float) (centerY + baseRadius * Math.sin(Math.toRadians(angle)));
            float endX = (float) (centerX + (baseRadius + barLength) * Math.cos(Math.toRadians(angle)));
            float endY = (float) (centerY + (baseRadius + barLength) * Math.sin(Math.toRadians(angle)));
            canvas.drawLine(startX, startY, endX, endY, barPaint);
        }
    }
}
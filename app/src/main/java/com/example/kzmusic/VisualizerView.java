// File: VisualizerView.java
package com.example.kzmusic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class VisualizerView extends View {
    public enum VisualizerMode {
        FOLDED, MIRRORED, PULSE, JUMBLED, SPLIT
    }
    public enum Orientation {
        TOP, TOP_RIGHT, RIGHT, BOTTOM_RIGHT, BOTTOM, BOTTOM_LEFT, LEFT, TOP_LEFT
    }
    private float rotationOffset = -90;
    private VisualizerMode currentMode = VisualizerMode.FOLDED;

    private float[] magnitudes;
    private Paint barPaint = new Paint();
    private static final int BAR_COUNT = 60;
    private float[] smoothedMagnitudes;
    private Random random = new Random();

    private int[] jumbledIndices = new int[BAR_COUNT];

    public VisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        barPaint.setColor(Color.WHITE);
        barPaint.setAntiAlias(true);
        barPaint.setStrokeCap(Paint.Cap.BUTT);
        smoothedMagnitudes = new float[BAR_COUNT];
    }

    // This public method is now more powerful
    public void setVisualizerMode(VisualizerMode mode) {
        this.currentMode = mode;
    }
    public void setOrientation(Orientation orientation) {
        switch (orientation) {
            case TOP:
                this.rotationOffset = -90; // 12 o'clock
                break;
            case TOP_RIGHT:
                this.rotationOffset = -45; // ~1:30 o'clock
                break;
            case RIGHT:
                this.rotationOffset = 0;   // 3 o'clock
                break;
            case BOTTOM_RIGHT:
                this.rotationOffset = 45;  // ~4:30 o'clock
                break;
            case BOTTOM:
                this.rotationOffset = 90;  // 6 o'clock
                break;
            case BOTTOM_LEFT:
                this.rotationOffset = 135; // ~7:30 o'clock
                break;
            case LEFT:
                this.rotationOffset = 180; // 9 o'clock
                break;
            case TOP_LEFT:
                this.rotationOffset = 225; // ~10:30 o'clock
                break;
        }
        invalidate(); // Redraw with the new orientation
    }

    public void updateVisualizer(float[] freshMagnitudes) {
        if (freshMagnitudes == null) return;
        this.magnitudes = freshMagnitudes;
        invalidate();
    }
    private boolean isSplitSpectrumClockwise = true;

    public void setSplitSpectrumDirection(boolean isClockwise) {
        this.isSplitSpectrumClockwise = isClockwise;
        invalidate(); // Redraw with the new direction
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
            case SPLIT: // Add the new case
                drawSplitSpectrum(canvas, centerX, centerY, baseRadius, maxBarLength);
                break;
        }
    }
    private void drawFoldedSpectrum(Canvas canvas, float centerX, float centerY, float baseRadius, float maxBarLength) {
        for (int i = 0; i <= BAR_COUNT; i++) {
            float angle = (float) (i * 360.0 / BAR_COUNT) + rotationOffset;
            int wrappedIndex = i % BAR_COUNT;
            int magnitudeIndex;
            if (wrappedIndex < BAR_COUNT / 2) { magnitudeIndex = wrappedIndex; } else { magnitudeIndex = BAR_COUNT - wrappedIndex; }
            magnitudeIndex = (int) ((magnitudeIndex / (float) (BAR_COUNT / 2)) * (magnitudes.length - 1));
            if (magnitudeIndex >= magnitudes.length) magnitudeIndex = magnitudes.length - 1;
            float magnitude = Math.min(magnitudes[magnitudeIndex] * 3.0f, 1.0f);
            smoothedMagnitudes[wrappedIndex] += (magnitude - smoothedMagnitudes[wrappedIndex]) * 0.4f;
            float barLength = smoothedMagnitudes[wrappedIndex] * maxBarLength;
            float startX = (float) (centerX + baseRadius * Math.cos(Math.toRadians(angle)));
            float startY = (float) (centerY + baseRadius * Math.sin(Math.toRadians(angle)));
            float endX = (float) (centerX + (baseRadius + barLength) * Math.cos(Math.toRadians(angle)));
            float endY = (float) (centerY + (baseRadius + barLength) * Math.sin(Math.toRadians(angle)));
            canvas.drawLine(startX, startY, endX, endY, barPaint);
        }
    }

    private void drawMirroredSpectrum(Canvas canvas, float centerX, float centerY, float baseRadius, float maxBarLength) {
        int halfBarCount = BAR_COUNT / 2;
        for (int i = 0; i <= halfBarCount; i++) {
            int magnitudeIndex = (int) (Math.pow(i / (float) halfBarCount, 2) * (magnitudes.length - 1));
            float magnitude = Math.min(magnitudes[magnitudeIndex] * 3.0f, 1.0f);
            smoothedMagnitudes[i + halfBarCount] += (magnitude - smoothedMagnitudes[i + halfBarCount]) * 0.4f;
            float barLength = smoothedMagnitudes[i + halfBarCount] * maxBarLength;
            float angleLeft = rotationOffset - (float)(i * 180.0 / halfBarCount);
            float angleRight = rotationOffset + (float)(i * 180.0 / halfBarCount);

            // Draw Left Bar
            float startX = (float) (centerX + baseRadius * Math.cos(Math.toRadians(angleLeft)));
            float startY = (float) (centerY + baseRadius * Math.sin(Math.toRadians(angleLeft)));
            float endX = (float) (centerX + (baseRadius + barLength) * Math.cos(Math.toRadians(angleLeft)));
            float endY = (float) (centerY + (baseRadius + barLength) * Math.sin(Math.toRadians(angleLeft)));
            canvas.drawLine(startX, startY, endX, endY, barPaint);

            // Draw Right Bar
            float mirroredStartX = (float) (centerX + baseRadius * Math.cos(Math.toRadians(angleRight)));
            float mirroredStartY = (float) (centerY + baseRadius * Math.sin(Math.toRadians(angleRight)));
            float mirroredEndX = (float) (centerX + (baseRadius + barLength) * Math.cos(Math.toRadians(angleRight)));
            float mirroredEndY = (float) (centerY + (baseRadius + barLength) * Math.sin(Math.toRadians(angleRight)));
            canvas.drawLine(mirroredStartX, mirroredStartY, mirroredEndX, mirroredEndY, barPaint);
        }
    }

    private void drawSplitSpectrum(Canvas canvas, float centerX, float centerY, float baseRadius, float maxBarLength) {
        int halfBarCount = BAR_COUNT / 2;
        int halfMagnitudes = magnitudes.length / 2;

        // Draw Left Half
        for (int i = 0; i < halfBarCount; i++) {
            float angle = (90 - (float)(i * 180.0 / (halfBarCount - 1))) + rotationOffset;
            int magnitudeIndex = (int) (((float)i / (halfBarCount - 1)) * halfMagnitudes);
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

        // Draw Right Half
        for (int i = 0; i < halfBarCount; i++) {
            float angle = (90 + (float)(i * 180.0 / (halfBarCount - 1))) + rotationOffset;
            int magnitudeIndex = halfMagnitudes + (int) (((float)i / (halfBarCount - 1)) * halfMagnitudes);
            if (magnitudeIndex >= magnitudes.length) magnitudeIndex = magnitudes.length - 1;
            float magnitude = Math.min(magnitudes[magnitudeIndex] * 3.0f, 1.0f);
            smoothedMagnitudes[i + halfBarCount] += (magnitude - smoothedMagnitudes[i + halfBarCount]) * 0.4f;
            float barLength = smoothedMagnitudes[i + halfBarCount] * maxBarLength;
            if (barLength < 2f) barLength = 2f;
            float startX = (float) (centerX + baseRadius * Math.cos(Math.toRadians(angle)));
            float startY = (float) (centerY + baseRadius * Math.sin(Math.toRadians(angle)));
            float endX = (float) (centerX + (baseRadius + barLength) * Math.cos(Math.toRadians(angle)));
            float endY = (float) (centerY + (baseRadius + barLength) * Math.sin(Math.toRadians(angle)));
            canvas.drawLine(startX, startY, endX, endY, barPaint);
        }
    }

    // Pulse and Jumbled are symmetrical by nature, so they just need the offset
    private void drawPulse(Canvas canvas, float centerX, float centerY, float baseRadius, float maxBarLength) {
        float bassMagnitude = 0; int bassBins = Math.min(8, magnitudes.length);
        for (int i = 0; i < bassBins; i++) { bassMagnitude += magnitudes[i]; }
        bassMagnitude /= bassBins;
        float magnitude = Math.min(bassMagnitude * 5.0f, 1.0f);
        smoothedMagnitudes[0] += (magnitude - smoothedMagnitudes[0]) * 0.4f;
        float barLength = smoothedMagnitudes[0] * maxBarLength;
        for (int i = 0; i <= BAR_COUNT; i++) {
            float angle = (float) (i * 360.0 / BAR_COUNT) + rotationOffset;
            float startX = (float) (centerX + baseRadius * Math.cos(Math.toRadians(angle)));
            float startY = (float) (centerY + baseRadius * Math.sin(Math.toRadians(angle)));
            float endX = (float) (centerX + (baseRadius + barLength) * Math.cos(Math.toRadians(angle)));
            float endY = (float) (centerY + (baseRadius + barLength) * Math.sin(Math.toRadians(angle)));
            canvas.drawLine(startX, startY, endX, endY, barPaint);
        }
    }
    private void drawJumbled(Canvas canvas, float centerX, float centerY, float baseRadius, float maxBarLength) {
        for (int i = 0; i <= BAR_COUNT; i++) {
            float angle = (float) (i * 360.0 / BAR_COUNT) + rotationOffset;
            int wrappedIndex = i % BAR_COUNT; int magnitudeIndex = random.nextInt(magnitudes.length);
            float magnitude = Math.min(magnitudes[magnitudeIndex] * 3.0f, 1.0f);
            smoothedMagnitudes[wrappedIndex] += (magnitude - smoothedMagnitudes[wrappedIndex]) * 0.4f;
            float barLength = smoothedMagnitudes[wrappedIndex] * maxBarLength;
            float startX = (float) (centerX + baseRadius * Math.cos(Math.toRadians(angle)));
            float startY = (float) (centerY + baseRadius * Math.sin(Math.toRadians(angle)));
            float endX = (float) (centerX + (baseRadius + barLength) * Math.cos(Math.toRadians(angle)));
            float endY = (float) (centerY + (baseRadius + barLength) * Math.sin(Math.toRadians(angle)));
            canvas.drawLine(startX, startY, endX, endY, barPaint);
        }
    }
}
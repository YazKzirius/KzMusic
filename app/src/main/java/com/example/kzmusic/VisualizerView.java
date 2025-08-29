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
    private static final int BAR_COUNT = 48; // Number of bars to draw
    private float[] smoothedMagnitudes; // For smoother animations

    public VisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        barPaint.setColor(Color.WHITE); // Set your desired bar color here
        barPaint.setAntiAlias(true);
        smoothedMagnitudes = new float[BAR_COUNT];
    }

    // This is the ONLY public method. The UI calls this to feed it new data.
    public void updateVisualizer(float[] freshMagnitudes) {
        if (freshMagnitudes == null) return;
        this.magnitudes = freshMagnitudes;
        invalidate(); // IMPORTANT: This tells the View it needs to redraw itself.
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Guard clause: Don't draw if the view isn't ready or has no data.
        if (magnitudes == null || getWidth() == 0) {
            return; // Prevents crashing and unnecessary drawing
        }

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float baseRadius = getWidth() * 0.25f; // Radius for the album art circle
        float maxBarLength = getWidth() * 0.18f; // The longest a bar can be

        barPaint.setStrokeWidth(getWidth() / 120f); // Responsive bar width

        for (int i = 0; i < BAR_COUNT; i++) {
            // Calculate the angle for the current bar
            float angle = (float) (i * 360.0 / BAR_COUNT);

            // --- THIS IS THE KEY DRAWING LOGIC ---
            // 1. Sample the frequency data. A power scale makes bass more prominent.
            int magnitudeIndex = (int) (Math.pow((i + 1) / (float)BAR_COUNT, 2) * (magnitudes.length - 1));
            // 2. Amplify and normalize the magnitude (clamp between 0 and 1).
            float magnitude = Math.min(magnitudes[magnitudeIndex] * 3.0f, 1.0f);

            // 3. Smooth the animation. Each bar moves towards its new height instead of jumping instantly.
            smoothedMagnitudes[i] += (magnitude - smoothedMagnitudes[i]) * 0.4f;

            // 4. Calculate the bar's length based on the smoothed magnitude.
            float barLength = smoothedMagnitudes[i] * maxBarLength;

            // 5. Calculate the start and end coordinates for drawing the line.
            float startX = (float) (centerX + baseRadius * Math.cos(Math.toRadians(angle)));
            float startY = (float) (centerY + baseRadius * Math.sin(Math.toRadians(angle)));
            float endX = (float) (centerX + (baseRadius + barLength) * Math.cos(Math.toRadians(angle)));
            float endY = (float) (centerY + (baseRadius + barLength) * Math.sin(Math.toRadians(angle)));

            // 6. Draw the line.
            canvas.drawLine(startX, startY, endX, endY, barPaint);
        }
    }
}
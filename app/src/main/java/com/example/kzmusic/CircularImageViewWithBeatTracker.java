package com.example.kzmusic;

//Imports
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

//This class creates an implements a circular imageview for album covers with beat bars around
public class CircularImageViewWithBeatTracker extends AppCompatImageView {
    //View attributes
    private Paint imagePaint;
    private Paint beatPaint;
    private BitmapShader shader;
    private float[] beatLevels;
    private Bitmap bitmap;

    public CircularImageViewWithBeatTracker(Context context) {
        super(context);
        init();
    }

    public CircularImageViewWithBeatTracker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularImageViewWithBeatTracker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    //This function intialises view attributes and creates the image
    private void init() {
        imagePaint = new Paint();
        imagePaint.setAntiAlias(true);

        beatPaint = new Paint();
        beatPaint.setAntiAlias(true);
        beatPaint.setColor(getResources().getColor(android.R.color.white));
        beatPaint.setStyle(Paint.Style.STROKE);
        beatPaint.setStrokeWidth(10f);

        // Initialize beat levels
        beatLevels = new float[150]; // Adjust the number of beat indicators as needed
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap != null) {
            if (shader == null) {
                shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                imagePaint.setShader(shader);
            }

            float radius = Math.min(getWidth() / 2.0f, getHeight() / 2.0f);
            canvas.drawCircle(getWidth() / 2.0f, getHeight() / 2.0f, radius, imagePaint);

            // Draw beat tracker
            drawBeatTracker(canvas, radius);
        } else {
            super.onDraw(canvas);
        }
    }
    //This function draws the beat tracker around the circumference of the circle
    private void drawBeatTracker(Canvas canvas, float radius) {
        for (int i = 0; i < beatLevels.length; i++) {
            float angle = (float) (i * (360.0 / beatLevels.length));
            float beatRadius = radius + 20 + (beatLevels[i] * 20); // Adjust the beat radius and multiplier as needed

            float startX = getWidth() / 2.0f + (float) (radius * Math.cos(Math.toRadians(angle)));
            float startY = getHeight() / 2.0f + (float) (radius * Math.sin(Math.toRadians(angle)));

            float stopX = getWidth() / 2.0f + (float) (beatRadius * Math.cos(Math.toRadians(angle)));
            float stopY = getHeight() / 2.0f + (float) (beatRadius * Math.sin(Math.toRadians(angle)));

            canvas.drawLine(startX, startY, stopX, stopY, beatPaint);
        }
    }
    //This function updates the beat bar levels in the imageview
    public void updateBeatLevels(float[] newLevels) {
        System.arraycopy(newLevels, 0, beatLevels, 0, beatLevels.length);
        invalidate(); // Redraw the view with updated beat levels
    }
    //This function loads an image based on it's url to the custom image view
    public void loadImage(Uri imageUrl) {
        if (imageUrl.toString().equals("content://media/external/audio/albumart/7616398988556461978")){
            loadImageResource(R.drawable.ic_library);
        } else {
            Glide.with(getContext())
                    .asBitmap()
                    .load(imageUrl)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                            bitmap = resource;
                            shader = null; // Reset shader to update the image
                            invalidate();
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
        }
    }
    //This function loads a random image resource
    private void loadImageResource(int resId) {
        Glide.with(getContext())
                .asBitmap()
                .load(resId)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        bitmap = resource;
                        shader = null; // Reset shader to update the image
                        invalidate();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }
}

package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Pure tvOS / Apple TV Floating Focus Highlight:
 * A subtle, physical floating glass frame with a crisp 1.5dp sapphire rim,
 * soft top specular gradient, and smooth diffuse outer glow.
 * Cached in hardware layer for locked 60fps translation across tiles on Fire TV.
 */
public class GlassView extends View {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint specular = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private float radius;
    /** Glow colour: Sapphire cyan (0x38BDF8), or festival's accent while active. */
    private int glowRgb = 0x38BDF8;
    private int glowAlpha = 0x44;

    public GlassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        radius = 16f * density;
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(1.5f * density);
        rim.setColor(0xE638BDF8);
        glow.setStyle(Paint.Style.STROKE);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    /** Tints the rim and outer glow; 0 restores default sapphire. */
    void setAccent(int argb) {
        if (argb == 0) {
            glowRgb = 0x38BDF8;
            glowAlpha = 0x44;
            rim.setColor(0xE638BDF8);
        } else {
            glowRgb = argb & 0xFFFFFF;
            glowAlpha = 0x60;
            int r = (((glowRgb >> 16) & 0xFF) + 255 * 2) / 3;
            int g = (((glowRgb >> 8) & 0xFF) + 255 * 2) / 3;
            int b = ((glowRgb & 0xFF) + 255 * 2) / 3;
            rim.setColor(0xE6000000 | (r << 16) | (g << 8) | b);
        }
        invalidate();
    }

    void setRadius(float px) {
        radius = px;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fill.setShader(new LinearGradient(0, 0, 0, h, 0x1A1E3A5F, 0x050B1A2F, Shader.TileMode.CLAMP));
        specular.setShader(new LinearGradient(0, 0, 0, h * 0.38f, 0x3D38BDF8, 0x0038BDF8, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float inset = 6f * density;
        rect.set(inset, inset, getWidth() - inset, getHeight() - inset);

        // Soft atmospheric diffuse glow: 2 subtle widening strokes
        for (int i = 1; i <= 2; i++) {
            glow.setStrokeWidth(i * 2.5f * density);
            glow.setColor((glowAlpha / i) << 24 | glowRgb);
            float o = i * density;
            canvas.drawRoundRect(rect.left - o, rect.top - o, rect.right + o, rect.bottom + o, radius + o, radius + o, glow);
        }

        // Translucent sheen fill
        canvas.drawRoundRect(rect, radius, radius, fill);

        // Top specular sheen
        canvas.save();
        canvas.clipRect(rect.left, rect.top, rect.right, rect.top + rect.height() * 0.38f);
        canvas.drawRoundRect(rect, radius, radius, specular);
        canvas.restore();

        // Single, elegant sapphire rim
        canvas.drawRoundRect(rect, radius, radius, rim);
    }
}

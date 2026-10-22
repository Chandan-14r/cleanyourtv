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
 * Cinematic Floating Glass Highlight:
 * A translucent floating glass slab with soft sheen fill, subtle top specular sheen,
 * restrained light sweep / edge highlight, crisp thin sapphire border, and layered ambient glow.
 * Cached in hardware layer for silky 60fps translation across tiles on Fire TV hardware.
 */
public class GlassView extends View {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint specular = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint specularSweep = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private float radius;
    /** Glow colour: Sapphire cyan (0x38BDF8), or festival's accent while active. */
    private int glowRgb = 0x38BDF8;
    private int glowAlpha = 0x58;

    public GlassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        radius = 16f * density;
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(1.6f * density);
        rim.setColor(0xEE38BDF8);
        glow.setStyle(Paint.Style.STROKE);
        // Cache the slab as a texture. The spring only changes translation, so the GPU can
        // move it without replaying clipPath and the glow on every frame.
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    /** Tints the rim and the outer glow; 0 restores the sapphire cyan glass. */
    void setAccent(int argb) {
        if (argb == 0) {
            glowRgb = 0x38BDF8;
            glowAlpha = 0x58;
            rim.setColor(0xEE38BDF8);
        } else {
            glowRgb = argb & 0xFFFFFF;
            glowAlpha = 0x70;
            // The rim is the accent lifted towards white, so the edge still reads as glass, not paint.
            int r = (((glowRgb >> 16) & 0xFF) + 255 * 2) / 3, g = (((glowRgb >> 8) & 0xFF) + 255 * 2) / 3,
                    b = ((glowRgb & 0xFF) + 255 * 2) / 3;
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
        fill.setShader(new LinearGradient(0, 0, 0, h, 0x2E1E3A5F, 0x0A0B1A2F, Shader.TileMode.CLAMP));
        specular.setShader(new LinearGradient(0, 0, 0, h * 0.42f, 0x5538BDF8, 0x0038BDF8, Shader.TileMode.CLAMP));
        // Restrained light sweep / edge highlight across the top-right
        specularSweep.setShader(new LinearGradient(w * 0.35f, 0, w, 0, 0x0038BDF8, 0x66E0F2FE, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float inset = 6f * density;   // room for the glow
        rect.set(inset, inset, getWidth() - inset, getHeight() - inset);

        // Outer glow: 3 widening, fading strokes for atmospheric bloom
        for (int i = 1; i <= 3; i++) {
            glow.setStrokeWidth(i * 2.2f * density);
            glow.setColor((glowAlpha / i) << 24 | glowRgb);
            float o = i * density;
            canvas.drawRoundRect(rect.left - o, rect.top - o, rect.right + o, rect.bottom + o, radius + o, radius + o, glow);
        }

        // Translucent glass fill
        canvas.drawRoundRect(rect, radius, radius, fill);

        // Specular sheen across the top 42%
        canvas.save();
        canvas.clipRect(rect.left, rect.top, rect.right, rect.top + rect.height() * 0.42f);
        canvas.drawRoundRect(rect, radius, radius, specular);
        canvas.restore();

        // Subtle restrained light sweep along the top edge
        canvas.save();
        canvas.clipRect(rect.left + rect.width() * 0.35f, rect.top, rect.right, rect.top + 3f * density);
        canvas.drawRoundRect(rect, radius, radius, specularSweep);
        canvas.restore();

        // Thin elegant sapphire rim
        canvas.drawRoundRect(rect, radius, radius, rim);
    }
}

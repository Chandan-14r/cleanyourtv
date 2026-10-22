package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import androidx.annotation.Nullable;

import java.util.Random;

/**
 * Manages Home Screen visual aesthetics:
 * 1. Rich ambient vignette gradients (Sapphire, Ruby, Emerald, Amethyst, Amber, Stark Crimson, Asgardian Gold, etc.).
 * 2. Over 20 curated 4K TV wallpapers including Marvel / Avengers themed artworks and modern geometric scenes.
 * 3. Seamless switching and live preview between Gradient and Wallpaper modes.
 */
public class BackdropThemeManager {
    private static final String PREFS_NAME = "backdrop_prefs";
    private static final String KEY_MODE = "backdrop_mode"; // "gradient" or "wallpaper"
    private static final String KEY_GRADIENT = "gradient_preset";
    private static final String KEY_WALLPAPER = "wallpaper_preset";

    public static final String MODE_GRADIENT = "gradient";
    public static final String MODE_WALLPAPER = "wallpaper";

    // Gradient Presets
    public static final String GRAD_SAPPHIRE = "sapphire"; // Default
    public static final String GRAD_DYNAMIC = "dynamic";
    public static final String GRAD_RUBY = "ruby";
    public static final String GRAD_EMERALD = "emerald";
    public static final String GRAD_AMETHYST = "amethyst";
    public static final String GRAD_AMBER = "amber";
    public static final String GRAD_CRIMSON_STARK = "crimson_stark";
    public static final String GRAD_ASGARDIAN_GOLD = "asgardian_gold";
    public static final String GRAD_VIBRANIUM = "vibranium";
    public static final String GRAD_QUANTUM_CYAN = "quantum_cyan";
    public static final String GRAD_NEON_CYBER = "neon_cyber";
    public static final String GRAD_OLED = "oled";

    // Marvel / Avengers Wallpaper Presets
    public static final String WALL_AVENGERS = "avengers_logo";
    public static final String WALL_IRON_MAN = "iron_man";
    public static final String WALL_CAPTAIN_AMERICA = "captain_america";
    public static final String WALL_THOR = "thor";
    public static final String WALL_SPIDER_MAN = "spider_man";
    public static final String WALL_BLACK_PANTHER = "black_panther";
    public static final String WALL_INFINITY = "infinity_stones";
    public static final String WALL_HULK = "hulk";
    public static final String WALL_DR_STRANGE = "dr_strange";
    public static final String WALL_QUANTUM = "quantum_realm";

    // Modern 4K Nature & Abstract Wallpaper Presets
    public static final String WALL_SPACE = "space";
    public static final String WALL_PEAKS = "peaks";
    public static final String WALL_OBSIDIAN = "obsidian";
    public static final String WALL_AURORA = "aurora";
    public static final String WALL_SYNTHWAVE = "synthwave";
    public static final String WALL_MATRIX = "matrix";
    public static final String WALL_SUPERNOVA = "supernova";
    public static final String WALL_DESERT_DUNE = "desert_dune";
    public static final String WALL_ABYSS = "abyss";
    public static final String WALL_GEOMETRY_LUX = "geometry_lux";
    public static final String WALL_CARBON_HEX = "carbon_hex";
    public static final String WALL_SAKURA = "sakura";
    public static final String WALL_CRYSTALS = "crystals";
    public static final String WALL_CIRCUIT = "circuit";

    private static BackdropThemeManager instance;
    private final SharedPreferences prefs;
    private Bitmap cachedBitmap;
    private String cachedKey = "";

    private BackdropThemeManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized BackdropThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new BackdropThemeManager(context);
        }
        return instance;
    }

    public String getMode() {
        return prefs.getString(KEY_MODE, MODE_GRADIENT);
    }

    public void setMode(String mode) {
        prefs.edit().putString(KEY_MODE, mode).apply();
        clearCache();
    }

    public String getGradientPreset() {
        String preset = prefs.getString(KEY_GRADIENT, GRAD_SAPPHIRE);
        if (GRAD_RUBY.equals(preset)) {
            prefs.edit().putString(KEY_GRADIENT, GRAD_SAPPHIRE).apply();
            return GRAD_SAPPHIRE;
        }
        return preset;
    }

    public void setGradientPreset(String preset) {
        prefs.edit().putString(KEY_GRADIENT, preset).apply();
        clearCache();
    }

    public String getWallpaperPreset() {
        return prefs.getString(KEY_WALLPAPER, WALL_AVENGERS);
    }

    public void setWallpaperPreset(String preset) {
        prefs.edit().putString(KEY_WALLPAPER, preset).apply();
        clearCache();
    }

    public void clearCache() {
        if (cachedBitmap != null) {
            cachedBitmap.recycle();
            cachedBitmap = null;
        }
        cachedKey = "";
    }

    /**
     * Renders the current backdrop bitmap (low CPU, high performance 960x540 buffer stretched to 1080p/4K).
     */
    public Bitmap getBackdropBitmap(int screenWidth, int screenHeight, @Nullable Integer dynamicAppColor) {
        String mode = getMode();
        String preset = MODE_WALLPAPER.equals(mode) ? getWallpaperPreset() : getGradientPreset();
        int dyn = (dynamicAppColor != null) ? dynamicAppColor : 0;
        String key = mode + "_" + preset + "_" + (GRAD_DYNAMIC.equals(preset) ? dyn : 0);

        if (cachedBitmap != null && key.equals(cachedKey)) {
            return cachedBitmap;
        }

        int w = 960;
        int h = 540;

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        if (MODE_WALLPAPER.equals(mode)) {
            renderWallpaper(canvas, w, h, preset);
        } else {
            renderGradient(canvas, w, h, preset, dynamicAppColor);
        }

        if (cachedBitmap != null) cachedBitmap.recycle();
        cachedBitmap = bmp;
        cachedKey = key;
        return bmp;
    }

    private void renderGradient(Canvas canvas, int w, int h, String preset, @Nullable Integer dynamicColor) {
        int bgColor = 0xFF07111F;
        int primaryColor = 0xFF1E4E8C;

        if (GRAD_SAPPHIRE.equals(preset)) {
            canvas.drawColor(0xFF07111F);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new RadialGradient(w * 0.32f, h * 0.38f, w * 0.70f,
                    new int[]{0x991E4E8C, 0x4D123158, 0x1A091C33, 0x00000000},
                    new float[]{0f, 0.40f, 0.75f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);

            paint.setShader(new RadialGradient(w * 0.80f, h * 0.22f, w * 0.42f,
                    new int[]{0x3338BDF8, 0x140284C7, 0x00000000},
                    new float[]{0f, 0.50f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);

            paint.setShader(new RadialGradient(w * 0.5f, h * 0.5f, w * 0.75f,
                    new int[]{0x00000000, 0x66030914},
                    new float[]{0.55f, 1.0f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            return;
        } else if (GRAD_CRIMSON_STARK.equals(preset)) {
            // Stark Industries Crimson & Gold
            canvas.drawColor(0xFF0C0304);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new RadialGradient(w * 0.30f, h * 0.40f, w * 0.65f,
                    new int[]{0xAA990011, 0x5555000A, 0x00000000},
                    new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(new RadialGradient(w * 0.75f, h * 0.25f, w * 0.40f,
                    new int[]{0x44FFB300, 0x00000000},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            return;
        } else if (GRAD_ASGARDIAN_GOLD.equals(preset)) {
            // Thor Midnight & Electric Gold
            canvas.drawColor(0xFF04060E);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new RadialGradient(w * 0.35f, h * 0.35f, w * 0.65f,
                    new int[]{0x882A3A60, 0x33121C35, 0x00000000},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(new RadialGradient(w * 0.70f, h * 0.30f, w * 0.45f,
                    new int[]{0x44FFD700, 0x00000000},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            return;
        } else if (GRAD_VIBRANIUM.equals(preset)) {
            // Wakanda Vibranium Purple
            canvas.drawColor(0xFF07040C);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new RadialGradient(w * 0.32f, h * 0.38f, w * 0.68f,
                    new int[]{0x885B2C6F, 0x33281133, 0x00000000},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(new RadialGradient(w * 0.75f, h * 0.25f, w * 0.40f,
                    new int[]{0x448E44AD, 0x00000000},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            return;
        } else if (GRAD_QUANTUM_CYAN.equals(preset)) {
            // Quantum Realm Cyan & Teal
            canvas.drawColor(0xFF02090C);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new RadialGradient(w * 0.32f, h * 0.38f, w * 0.65f,
                    new int[]{0x8800838F, 0x3300363A, 0x00000000},
                    new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(new RadialGradient(w * 0.75f, h * 0.25f, w * 0.40f,
                    new int[]{0x4400E5FF, 0x00000000},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            return;
        } else if (GRAD_NEON_CYBER.equals(preset)) {
            // Cyberpunk Magenta & Cyan
            canvas.drawColor(0xFF09040D);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new RadialGradient(w * 0.25f, h * 0.40f, w * 0.60f,
                    new int[]{0x77E91E63, 0x00000000},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(new RadialGradient(w * 0.78f, h * 0.30f, w * 0.50f,
                    new int[]{0x5500E5FF, 0x00000000},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            return;
        } else if (GRAD_RUBY.equals(preset)) {
            bgColor = 0xFF070507;
            primaryColor = 0xFFD32F2F;
        } else if (GRAD_EMERALD.equals(preset)) {
            bgColor = 0xFF040907;
            primaryColor = 0xFF00897B;
        } else if (GRAD_AMETHYST.equals(preset)) {
            bgColor = 0xFF08040C;
            primaryColor = 0xFF8E24AA;
        } else if (GRAD_AMBER.equals(preset)) {
            bgColor = 0xFF0C0604;
            primaryColor = 0xFFF4511E;
        } else if (GRAD_DYNAMIC.equals(preset)) {
            bgColor = 0xFF060608;
            primaryColor = (dynamicColor != null && dynamicColor != -1) ? dynamicColor : 0xFF1E4E8C;
        } else if (GRAD_OLED.equals(preset)) {
            canvas.drawColor(0xFF000000);
            return;
        }

        canvas.drawColor(bgColor);
        float cx = w * 0.30f;
        float cy = h * 0.40f;
        float radius = w * 0.65f;

        int r = (primaryColor >> 16) & 0xFF;
        int g = (primaryColor >> 8) & 0xFF;
        int b = primaryColor & 0xFF;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new RadialGradient(cx, cy, radius,
                new int[]{Color.argb(145, r, g, b), Color.argb(65, r, g, b), Color.argb(20, r, g, b), Color.argb(0, r, g, b)},
                new float[]{0f, 0.35f, 0.70f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);

        paint.setShader(new RadialGradient(w * 0.5f, h * 0.5f, w * 0.75f,
                new int[]{0x00000000, 0x88000000},
                new float[]{0.5f, 1.0f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
    }

    private void renderWallpaper(Canvas canvas, int w, int h, String preset) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 1. AVENGERS ASSEMBLE LOGO
        if (WALL_AVENGERS.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, w, h, 0xFF070913, 0xFF101626, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            // Ambient Cyan & Silver Core
            p.setShader(new RadialGradient(w * 0.65f, h * 0.48f, w * 0.45f,
                    new int[]{0x6600E5FF, 0x220077AA, 0x00000000},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            // Large Iconic Avengers 'A' Crest at (w*0.65, h*0.48)
            float ax = w * 0.65f;
            float ay = h * 0.48f;
            float ar = h * 0.38f;

            // Outer Circle Ring
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(12f);
            p.setColor(0xCC00E5FF);
            canvas.drawCircle(ax, ay, ar, p);

            // Outer Brushed Dark Rim
            p.setStrokeWidth(4f);
            p.setColor(0x88FFFFFF);
            canvas.drawCircle(ax, ay, ar + 8f, p);

            // Slanted Avengers 'A' Emblem
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xE6FFFFFF);
            Path aPath = new Path();
            aPath.moveTo(ax - ar * 0.45f, ay + ar * 0.85f);
            aPath.lineTo(ax - ar * 0.15f, ay - ar * 0.85f);
            aPath.lineTo(ax + ar * 0.10f, ay - ar * 0.85f);
            aPath.lineTo(ax + ar * 0.65f, ay + ar * 0.85f);
            aPath.lineTo(ax + ar * 0.40f, ay + ar * 0.85f);
            aPath.lineTo(ax + ar * 0.28f, ay + ar * 0.45f);
            aPath.lineTo(ax - ar * 0.18f, ay + ar * 0.45f);
            aPath.lineTo(ax - ar * 0.30f, ay + ar * 0.85f);
            aPath.close();
            canvas.drawPath(aPath, p);

            // Arrow cutout on horizontal bar
            p.setColor(0xFF070913);
            Path arrow = new Path();
            arrow.moveTo(ax - ar * 0.30f, ay + ar * 0.18f);
            arrow.lineTo(ax + ar * 0.55f, ay + ar * 0.18f);
            arrow.lineTo(ax + ar * 0.40f, ay - ar * 0.05f);
            arrow.lineTo(ax + ar * 0.50f, ay + ar * 0.28f);
            arrow.lineTo(ax + ar * 0.40f, ay + ar * 0.60f);
            arrow.lineTo(ax + ar * 0.45f, ay + ar * 0.38f);
            arrow.lineTo(ax - ar * 0.30f, ay + ar * 0.38f);
            arrow.close();
            canvas.drawPath(arrow, p);
            p.setStyle(Paint.Style.FILL);

        // 2. IRON MAN ARC REACTOR
        } else if (WALL_IRON_MAN.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, 0, h, 0xFF0D0204, 0xFF1A0609, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            // Arc Reactor Blue Unibeam Glow
            float rx = w * 0.65f, ry = h * 0.50f;
            p.setShader(new RadialGradient(rx, ry, w * 0.40f,
                    new int[]{0x9900E5FF, 0x440077B6, 0x00000000},
                    new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            // Concentric Titanium & Cyan Reactor Rings
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setColor(0x88FFD700);
            p.setStrokeWidth(4f);
            canvas.drawCircle(rx, ry, 140f, p);

            p.setColor(0xCC00E5FF);
            p.setStrokeWidth(14f);
            canvas.drawCircle(rx, ry, 110f, p);

            p.setColor(0xFFFFFFFF);
            p.setStrokeWidth(6f);
            canvas.drawCircle(rx, ry, 80f, p);

            // 10 Radial Reactor Coils
            p.setStrokeWidth(8f);
            p.setColor(0xDDFFB300);
            for (int i = 0; i < 10; i++) {
                double rad = Math.toRadians(i * 36);
                float x1 = rx + (float) (Math.cos(rad) * 95);
                float y1 = ry + (float) (Math.sin(rad) * 95);
                float x2 = rx + (float) (Math.cos(rad) * 125);
                float y2 = ry + (float) (Math.sin(rad) * 125);
                canvas.drawLine(x1, y1, x2, y2, p);
            }
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFFFFFFF);
            canvas.drawCircle(rx, ry, 35f, p);

        // 3. CAPTAIN AMERICA VIBRANIUM SHIELD
        } else if (WALL_CAPTAIN_AMERICA.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, w, h, 0xFF050A14, 0xFF0D1B2A, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            float sx = w * 0.65f, sy = h * 0.50f;
            // Red Outer Ring
            p.setColor(0xFFB71C1C);
            canvas.drawCircle(sx, sy, 180f, p);
            // Silver Ring
            p.setColor(0xFFE0E0E0);
            canvas.drawCircle(sx, sy, 145f, p);
            // Red Inner Ring
            p.setColor(0xFFB71C1C);
            canvas.drawCircle(sx, sy, 110f, p);
            // Blue Star Core
            p.setColor(0xFF0D47A1);
            canvas.drawCircle(sx, sy, 75f, p);

            // Five-point Silver Star
            p.setColor(0xFFFFFFFF);
            Path star = new Path();
            float outerR = 65f, innerR = 26f;
            for (int i = 0; i < 10; i++) {
                float r = (i % 2 == 0) ? outerR : innerR;
                double angle = Math.toRadians(i * 36 - 90);
                float x = sx + (float) (Math.cos(angle) * r);
                float y = sy + (float) (Math.sin(angle) * r);
                if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
            }
            star.close();
            canvas.drawPath(star, p);

        // 4. THOR GOD OF THUNDER
        } else if (WALL_THOR.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, 0, h, 0xFF03050A, 0xFF080F1E, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            // Lightning Storm Flash
            p.setShader(new RadialGradient(w * 0.65f, h * 0.35f, w * 0.45f,
                    new int[]{0x884FC3F7, 0x330288D1, 0x00000000},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            // Forked Lightning Bolts
            p.setShader(null);
            p.setColor(0xFFE1F5FE);
            p.setStrokeWidth(4f);
            p.setStyle(Paint.Style.STROKE);
            Path bolt = new Path();
            bolt.moveTo(w * 0.65f, 0);
            bolt.lineTo(w * 0.63f, h * 0.22f);
            bolt.lineTo(w * 0.68f, h * 0.32f);
            bolt.lineTo(w * 0.62f, h * 0.52f);
            bolt.lineTo(w * 0.67f, h * 0.65f);
            bolt.lineTo(w * 0.64f, h);
            canvas.drawPath(bolt, p);

            // Secondary branches
            p.setStrokeWidth(2f);
            p.setColor(0x88B3E5FC);
            canvas.drawLine(w * 0.68f, h * 0.32f, w * 0.76f, h * 0.42f, p);
            canvas.drawLine(w * 0.62f, h * 0.52f, w * 0.54f, h * 0.62f, p);
            p.setStyle(Paint.Style.FILL);

        // 5. SPIDER-MAN NEON WEB
        } else if (WALL_SPIDER_MAN.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, w, h, 0xFF06070B, 0xFF14070A, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            float cx = w * 0.65f, cy = h * 0.50f;
            // Scarlet Neon Web
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2f);
            p.setColor(0x44D50000);
            for (int r = 30; r <= 240; r += 35) {
                canvas.drawCircle(cx, cy, r, p);
            }
            for (int a = 0; a < 360; a += 30) {
                double rad = Math.toRadians(a);
                canvas.drawLine(cx, cy, cx + (float)(Math.cos(rad) * 260), cy + (float)(Math.sin(rad) * 260), p);
            }
            // Stylized Scarlet Spider Emblem
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xEEFF1744);
            canvas.drawCircle(cx, cy, 18f, p);
            canvas.drawCircle(cx, cy - 24f, 12f, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(4f);
            p.setColor(0xEEFF1744);
            // 8 Spider Legs
            canvas.drawLine(cx - 10, cy - 10, cx - 45, cy - 35, p);
            canvas.drawLine(cx - 45, cy - 35, cx - 60, cy - 15, p);
            canvas.drawLine(cx + 10, cy - 10, cx + 45, cy - 35, p);
            canvas.drawLine(cx + 45, cy - 35, cx + 60, cy - 15, p);
            canvas.drawLine(cx - 12, cy + 10, cx - 48, cy + 45, p);
            canvas.drawLine(cx - 48, cy + 45, cx - 65, cy + 70, p);
            canvas.drawLine(cx + 12, cy + 10, cx + 48, cy + 45, p);
            canvas.drawLine(cx + 48, cy + 45, cx + 65, cy + 70, p);
            p.setStyle(Paint.Style.FILL);

        // 6. WAKANDA VIBRANIUM (BLACK PANTHER)
        } else if (WALL_BLACK_PANTHER.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, 0, h, 0xFF050308, 0xFF11071A, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            // Purple Kinetic Energy Glow
            p.setShader(new RadialGradient(w * 0.65f, h * 0.48f, w * 0.40f,
                    new int[]{0x777B1FA2, 0x224A148C, 0x00000000},
                    new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            // Wakandan Chevron Geometric Necklace
            p.setShader(null);
            p.setColor(0xCCBA68C8);
            p.setStyle(Paint.Style.FILL);
            float vx = w * 0.65f, vy = h * 0.45f;
            for (int i = -4; i <= 4; i++) {
                double rad = Math.toRadians(i * 18 + 90);
                float px = vx + (float)(Math.cos(rad) * 120);
                float py = vy + (float)(Math.sin(rad) * 90);
                Path fang = new Path();
                fang.moveTo(px - 8, py);
                fang.lineTo(px + 8, py);
                fang.lineTo(px, py + 38);
                fang.close();
                canvas.drawPath(fang, p);
            }

        // 7. INFINITY STONES COSMIC
        } else if (WALL_INFINITY.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, w, h, 0xFF030308, 0xFF0B0716, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            float gx = w * 0.65f, gy = h * 0.50f;
            int[] stoneColors = {0xFF9C27B0, 0xFF2196F3, 0xFFF44336, 0xFFFF9800, 0xFF4CAF50, 0xFFFFEB3B};
            for (int i = 0; i < 6; i++) {
                double rad = Math.toRadians(i * 60 - 30);
                float sx = gx + (float)(Math.cos(rad) * 130);
                float sy = gy + (float)(Math.sin(rad) * 110);
                p.setShader(new RadialGradient(sx, sy, 70f,
                        new int[]{Color.argb(200, Color.red(stoneColors[i]), Color.green(stoneColors[i]), Color.blue(stoneColors[i])), 0x00000000},
                        new float[]{0f, 1f}, Shader.TileMode.CLAMP));
                canvas.drawCircle(sx, sy, 70f, p);
                p.setShader(null);
                p.setColor(stoneColors[i]);
                canvas.drawCircle(sx, sy, 16f, p);
            }
            p.setShader(null);
            p.setColor(0xFFFFFFFF);
            canvas.drawCircle(gx, gy, 12f, p);

        // 8. HULK GAMMA OVERDRIVE
        } else if (WALL_HULK.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, 0, h, 0xFF020704, 0xFF07140B, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            p.setShader(new RadialGradient(w * 0.65f, h * 0.50f, w * 0.45f,
                    new int[]{0x8800E676, 0x221B5E20, 0x00000000},
                    new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(5f);
            p.setColor(0xCC69F0AE);
            Path fissure = new Path();
            fissure.moveTo(w * 0.65f, h * 0.15f);
            fissure.lineTo(w * 0.63f, h * 0.35f);
            fissure.lineTo(w * 0.69f, h * 0.50f);
            fissure.lineTo(w * 0.62f, h * 0.70f);
            fissure.lineTo(w * 0.66f, h * 0.90f);
            canvas.drawPath(fissure, p);
            p.setStyle(Paint.Style.FILL);

        // 9. DOCTOR STRANGE SANCTUM MANDALAS
        } else if (WALL_DR_STRANGE.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, w, h, 0xFF090401, 0xFF140A04, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            float mx = w * 0.65f, my = h * 0.50f;
            p.setShader(new RadialGradient(mx, my, w * 0.35f,
                    new int[]{0x88FF6D00, 0x22E65100, 0x00000000},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3f);
            p.setColor(0xDDFFD54F);
            canvas.drawCircle(mx, my, 130f, p);
            canvas.drawCircle(mx, my, 85f, p);
            for (int i = 0; i < 8; i++) {
                double rad = Math.toRadians(i * 45);
                float px = mx + (float)(Math.cos(rad) * 130);
                float py = my + (float)(Math.sin(rad) * 130);
                canvas.drawCircle(px, py, 22f, p);
            }
            p.setStyle(Paint.Style.FILL);

        // 10. QUANTUM REALM VORTICES
        } else if (WALL_QUANTUM.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, w, h, 0xFF0B0108, 0xFF021017, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            p.setShader(new RadialGradient(w * 0.60f, h * 0.45f, w * 0.45f,
                    new int[]{0x77FF007F, 0x3300E5FF, 0x00000000},
                    new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            Random rnd = new Random(42);
            p.setShader(null);
            for (int i = 0; i < 180; i++) {
                float qx = rnd.nextFloat() * w;
                float qy = rnd.nextFloat() * h;
                float qr = 1f + rnd.nextFloat() * 2.5f;
                p.setColor(i % 2 == 0 ? 0xBB00E5FF : 0xBBFF4081);
                canvas.drawCircle(qx, qy, qr, p);
            }

        // 11. 80S RETRO SYNTHWAVE
        } else if (WALL_SYNTHWAVE.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, 0, h, 0xFF0A0014, 0xFF2D0A3D, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            // Segmented Synthwave Sun
            float sunX = w * 0.65f, sunY = h * 0.45f, sunR = 100f;
            p.setShader(new LinearGradient(0, sunY - sunR, 0, sunY + sunR, 0xFFFFEE58, 0xFFE91E63, Shader.TileMode.CLAMP));
            canvas.drawCircle(sunX, sunY, sunR, p);

            // Sun segments
            p.setShader(null);
            p.setColor(0xFF1E0528);
            for (float sy = sunY; sy < sunY + sunR; sy += 14f) {
                canvas.drawRect(sunX - sunR, sy, sunX + sunR, sy + 4f, p);
            }

            // Wireframe Grid
            p.setColor(0x6600E5FF);
            p.setStrokeWidth(2f);
            float horizon = h * 0.65f;
            canvas.drawLine(0, horizon, w, horizon, p);
            for (int x = 0; x <= w; x += 60) {
                canvas.drawLine(sunX, horizon, x, h, p);
            }
            for (float y = horizon; y <= h; y += 18f) {
                canvas.drawLine(0, y, w, y, p);
            }

        // 12. CYBER MATRIX CODE STREAMS
        } else if (WALL_MATRIX.equals(preset)) {
            p.setColor(0xFF030704);
            canvas.drawRect(0, 0, w, h, p);
            p.setColor(0x9900E676);
            Random rnd = new Random(101);
            for (int x = 15; x < w; x += 22) {
                int len = 8 + rnd.nextInt(25);
                float startY = rnd.nextFloat() * (h - len * 16);
                for (int j = 0; j < len; j++) {
                    p.setAlpha(j == len - 1 ? 255 : (30 + j * 7));
                    canvas.drawRect(x, startY + j * 16, x + 8, startY + j * 16 + 10, p);
                }
            }

        // 13. SOLAR SUPERNOVA
        } else if (WALL_SUPERNOVA.equals(preset)) {
            p.setShader(new RadialGradient(w * 0.65f, h * 0.45f, w * 0.55f,
                    new int[]{0xFFFFD54F, 0xFFFF6F00, 0x44B71C1C, 0xFF050307},
                    new float[]{0f, 0.2f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

        // 14. MIDNIGHT SAND DUNES
        } else if (WALL_DESERT_DUNE.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, 0, h, 0xFF060914, 0xFF141A29, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            Path dune1 = new Path();
            dune1.moveTo(0, h * 0.60f);
            dune1.quadTo(w * 0.4f, h * 0.45f, w, h * 0.65f);
            dune1.lineTo(w, h); dune1.lineTo(0, h); dune1.close();
            p.setColor(0xFF0F1522);
            canvas.drawPath(dune1, p);

            Path dune2 = new Path();
            dune2.moveTo(0, h * 0.72f);
            dune2.quadTo(w * 0.6f, h * 0.58f, w, h * 0.78f);
            dune2.lineTo(w, h); dune2.lineTo(0, h); dune2.close();
            p.setColor(0xFF070B12);
            canvas.drawPath(dune2, p);

        // 15. BIOLUMINESCENT ABYSS
        } else if (WALL_ABYSS.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, 0, h, 0xFF01080E, 0xFF031622, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            p.setShader(new RadialGradient(w * 0.65f, h * 0.55f, w * 0.45f,
                    new int[]{0x6600B4D8, 0x1A0077B6, 0x00000000},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

        // 16. LUXURY GOLD & SLATE
        } else if (WALL_GEOMETRY_LUX.equals(preset)) {
            p.setColor(0xFF0A0C10);
            canvas.drawRect(0, 0, w, h, p);
            p.setColor(0x33FFD700);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2f);
            for (int x = -50; x < w + 100; x += 80) {
                for (int y = -50; y < h + 100; y += 80) {
                    canvas.drawRect(x, y, x + 65, y + 65, p);
                }
            }
            p.setStyle(Paint.Style.FILL);

        // 17. STEALTH CARBON HEX
        } else if (WALL_CARBON_HEX.equals(preset)) {
            p.setColor(0xFF07080A);
            canvas.drawRect(0, 0, w, h, p);
            p.setColor(0x22FFFFFF);
            for (int x = 0; x < w; x += 24) {
                for (int y = 0; y < h; y += 24) {
                    canvas.drawRect(x, y, x + 10, y + 10, p);
                }
            }

        // 18. NEON TOKYO SAKURA
        } else if (WALL_SAKURA.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, 0, h, 0xFF07040D, 0xFF170924, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            // Neon Pink Moon
            p.setShader(new RadialGradient(w * 0.70f, h * 0.35f, 90f,
                    new int[]{0xFFFF4081, 0x66F50057, 0x00000000},
                    new float[]{0f, 0.7f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(w * 0.70f, h * 0.35f, 90f, p);

        // 19. PRISMATIC CRYSTALS
        } else if (WALL_CRYSTALS.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, w, h, 0xFF05060D, 0xFF131024, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            p.setShader(new RadialGradient(w * 0.65f, h * 0.50f, w * 0.40f,
                    new int[]{0x667C4DFF, 0x22536DFE, 0x00000000},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

        // 20. SILICON CYBER CORE
        } else if (WALL_CIRCUIT.equals(preset)) {
            p.setColor(0xFF04060A);
            canvas.drawRect(0, 0, w, h, p);
            p.setColor(0x4400E5FF);
            p.setStrokeWidth(3f);
            for (int y = 50; y < h; y += 80) {
                canvas.drawLine(w * 0.4f, y, w * 0.65f, y, p);
                canvas.drawLine(w * 0.65f, y, w * 0.75f, y + 30, p);
                canvas.drawLine(w * 0.75f, y + 30, w, y + 30, p);
                canvas.drawCircle(w * 0.65f, y, 6f, p);
            }

        // 21. COSMIC DEEP SPACE
        } else if (WALL_SPACE.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, 0, h, 0xFF03040A, 0xFF0A0D1A, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            p.setShader(new RadialGradient(w * 0.35f, h * 0.45f, w * 0.45f,
                    new int[]{0x456A1B9A, 0x221565C0, 0x00000000},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            p.setShader(new RadialGradient(w * 0.75f, h * 0.30f, w * 0.35f,
                    new int[]{0x3500897B, 0x00000000},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            Random rnd = new Random(1337);
            Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            for (int i = 0; i < 220; i++) {
                float sx = rnd.nextFloat() * w;
                float sy = rnd.nextFloat() * h;
                float sr = 0.6f + rnd.nextFloat() * 1.4f;
                int alpha = 70 + rnd.nextInt(185);
                starPaint.setColor(Color.argb(alpha, 230, 240, 255));
                canvas.drawCircle(sx, sy, sr, starPaint);
            }

        // 22. MINIMALIST DARK PEAKS
        } else if (WALL_PEAKS.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, 0, h, 0xFF080C16, 0xFF14213D, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            p.setShader(new RadialGradient(w * 0.65f, h * 0.55f, w * 0.35f,
                    new int[]{0x55E0E8F5, 0x184A6572, 0x00000000},
                    new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            Path bgRidge = new Path();
            bgRidge.moveTo(0, h * 0.65f);
            bgRidge.lineTo(w * 0.22f, h * 0.50f);
            bgRidge.lineTo(w * 0.45f, h * 0.62f);
            bgRidge.lineTo(w * 0.68f, h * 0.46f);
            bgRidge.lineTo(w * 0.88f, h * 0.58f);
            bgRidge.lineTo(w, h * 0.52f);
            bgRidge.lineTo(w, h); bgRidge.lineTo(0, h); bgRidge.close();
            p.setShader(null);
            p.setColor(0xFF101B2B);
            canvas.drawPath(bgRidge, p);

            Path fgRidge = new Path();
            fgRidge.moveTo(0, h * 0.75f);
            fgRidge.lineTo(w * 0.32f, h * 0.58f);
            fgRidge.lineTo(w * 0.55f, h * 0.72f);
            fgRidge.lineTo(w * 0.78f, h * 0.56f);
            fgRidge.lineTo(w, h * 0.68f);
            fgRidge.lineTo(w, h); fgRidge.lineTo(0, h); fgRidge.close();
            p.setColor(0xFF070B12);
            canvas.drawPath(fgRidge, p);

        // 23. NORDIC AURORA BOREALIS
        } else if (WALL_AURORA.equals(preset)) {
            p.setShader(new LinearGradient(0, 0, 0, h, 0xFF03080E, 0xFF05131C, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            p.setShader(new RadialGradient(w * 0.40f, h * 0.38f, w * 0.50f,
                    new int[]{0x6500E676, 0x301DE9B6, 0x00000000},
                    new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paintOrNew(p));

            p.setShader(new RadialGradient(w * 0.55f, h * 0.22f, w * 0.40f,
                    new int[]{0x40AA00FF, 0x00000000},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            p.setShader(null);
            p.setColor(0xFF040A0F);
            canvas.drawRect(0, h * 0.78f, w, h, p);

        // 24. OBSIDIAN ABSTRACT WAVES (DEFAULT)
        } else {
            p.setShader(new LinearGradient(0, 0, w, h, 0xFF08080C, 0xFF151620, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            p.setShader(new RadialGradient(w * 0.25f, h * 0.35f, w * 0.45f,
                    new int[]{0x33455A64, 0x00000000},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);

            p.setShader(new RadialGradient(w * 0.70f, h * 0.65f, w * 0.40f,
                    new int[]{0x2237474F, 0x00000000},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, p);
        }

        // Soft 35% dark scrim across wallpapers so all icons & text maintain TV contrast
        p.setShader(null);
        p.setColor(0x59000000);
        canvas.drawRect(0, 0, w, h, p);
    }

    private Paint paintOrNew(Paint p) {
        return p != null ? p : new Paint(Paint.ANTI_ALIAS_FLAG);
    }
}

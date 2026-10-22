package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Enhanced tvOS / Google TV style dock decoration:
 * - Draws the translucent sapphire frosted floating glass dock behind the pinned dock row.
 * - Draws subtle surface lighting, fine sapphire border, and soft depth shadow.
 * - Draws clean section header: "✦ ALL APPLICATIONS" above the installed apps grid.
 */
public class DockDecoration extends RecyclerView.ItemDecoration {

    private final int dockSize;
    private final float density;
    private final Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint topHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint allAppsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF shadowRect = new RectF();

    DockDecoration(int dockSize, float density) {
        this.dockSize = dockSize;
        this.density = density;

        band.setColor(0xD4091628); // Translucent Sapphire Midnight Navy Glass
        shadow.setColor(0x38000000); // Soft ambient drop shadow

        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(1.2f * density);
        edge.setColor(0x2638BDF8); // Subtle Cyan-Sapphire luminous rim

        topHighlight.setStyle(Paint.Style.STROKE);
        topHighlight.setStrokeWidth(1.2f * density);
        topHighlight.setColor(0x3838BDF8); // Top specular sheen on the dock band

        allAppsPaint.setColor(0x9994A3B8); // Slate
        allAppsPaint.setTextSize(11f * density);
        allAppsPaint.setLetterSpacing(0.14f);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (pos >= 0 && pos < dockSize) {
            outRect.top = (int) (6 * density);
            outRect.bottom = (int) (12 * density);
        } else if (pos >= dockSize && pos < dockSize + dockSize) {
            outRect.top = (int) (30 * density);  // generous room for "✦ ALL APPLICATIONS"
            outRect.bottom = (int) (6 * density);
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        boolean any = false;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(child);
            if (pos < 0 || pos >= dockSize) continue;
            RecyclerView.ViewHolder vh = parent.getChildViewHolder(child);
            View card = vh instanceof AppAdapter.Holder ? ((AppAdapter.Holder) vh).card : child.findViewById(R.id.card);
            if (card == null) continue;
            float l = child.getLeft() + card.getLeft();
            float t = child.getTop() + card.getTop() + child.getTranslationY();
            float r = l + card.getWidth();
            float b = t + card.getHeight();
            if (!any) {
                rect.set(l, t, r, b);
                any = true;
            } else {
                rect.union(l, t, r, b);
            }
        }
        if (any) {
            float pad = 12 * density;
            rect.inset(-pad, -pad);
            float radius = 24 * density;

            // Soft outer shadow for floating elevation
            shadowRect.set(rect.left - 2 * density, rect.top + 4 * density, rect.right + 2 * density, rect.bottom + 8 * density);
            c.drawRoundRect(shadowRect, radius + 2 * density, radius + 2 * density, shadow);

            // Floating glass band
            c.drawRoundRect(rect, radius, radius, band);
            c.drawRoundRect(rect, radius, radius, edge);

            // Subtle top highlight
            c.save();
            c.clipRect(rect.left, rect.top, rect.right, rect.top + 3 * density);
            c.drawRoundRect(rect, radius, radius, topHighlight);
            c.restore();
        }

        // Draw "✦ ALL APPLICATIONS" section title above the first installed app row
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(child);
            if (pos == dockSize) {
                View card = child.findViewById(R.id.card);
                float textX = child.getLeft() + (card != null ? card.getLeft() : 0);
                float textY = child.getTop() + child.getTranslationY() - (10 * density);
                c.drawText("✦ ALL APPLICATIONS", textX, textY, allAppsPaint);
                break;
            }
        }
    }
}

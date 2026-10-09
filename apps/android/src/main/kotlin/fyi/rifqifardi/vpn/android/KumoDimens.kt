package fyi.rifqifardi.vpn.android

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout

/**
 * Cloudflare Kumo UI spacing / radius / stroke tokens for the Android view layer.
 *
 * Companion to [KumoColors] (colour tokens). WP3-prime introduces this because the view
 * code previously passed raw PIXEL literals to setPadding/setMargins/cornerRadius/
 * setStroke/minimumHeight. On a density-2.0 device those rendered at half their intent,
 * which collapsed inter-card separation and made 2px hairline strokes anti-alias into
 * invisibility against the #0D1117 canvas.
 *
 * PROVENANCE: these values are engineering-inferred from Kumo token intent, NOT lifted
 * from a published Kumo spacing scale (the design-system contract carries colours only).
 * They are reasonable defaults pending @product/design ratification — change them here,
 * not at call sites.
 *
 * Typography is deliberately absent: TextView.textSize already interprets its argument as
 * SP, so text sizes are not density bugs and stay at their call sites untouched.
 */
object KumoDimens {
    // Structural gutters & padding (dp)
    const val GutterOuter = 24f         // screen edge -> content column
    const val PaddingVerticalBase = 24f // top/bottom breathing room of the scroll column
    const val CardGap = 16f             // inter-card vertical margin
    const val CardPadding = 16f         // uniform inner padding of an elevated card
    const val HeroPaddingH = 20f        // hero panel horizontal inner padding
    const val HeroPaddingV = 24f        // hero panel vertical inner padding
    const val BadgePadH = 14f           // status badge horizontal padding
    const val BadgePadV = 6f            // status badge vertical padding
    const val BrandPillPadH = 10f       // "Cloudflare Kumo" tag horizontal padding
    const val BrandPillPadV = 3f        // "Cloudflare Kumo" tag vertical padding
    const val ButtonPadH = 24f          // connect button horizontal padding
    const val ButtonMinHeight = 48f     // touch-target floor for the primary control
    const val SpacerHeroToButton = 16f  // gap between status badge and connect button
    const val MetaLineGap = 6f          // gap between stacked profile metadata lines
    const val HeaderBottomGap = 24f     // gap below the sub-lead before the hero panel

    // Radii (dp)
    const val RadiusCard = 16f          // elevated card surfaces
    const val RadiusButton = 12f        // connect button

    // Strokes (dp) — must survive anti-aliasing at density 2.0
    const val StrokeWidth = 2f
}

// ── Top-level helpers ────────────────────────────────────────────────────────────────
// Declared outside the object so they are ordinary top-level functions/extensions that
// MainActivity can import and call directly. Member-extension functions of an object
// cannot be imported by FQN, which is why these live here rather than inside KumoDimens.

/** dp -> px float, for GradientDrawable.cornerRadius (takes px). */
fun Float.dpPx(context: Context): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics)

/** dp -> px int, for setPadding/setMargins/minimumHeight (take px). */
fun Float.dpInt(context: Context): Int = dpPx(context).toInt()

/** Uniform padding on all four sides. */
fun View.padAll(context: Context, dpValue: Float) {
    val px = dpValue.dpInt(context)
    setPadding(px, px, px, px)
}

/** Explicit LTRB padding in dp. */
fun View.padLtrb(context: Context, l: Float, t: Float, r: Float, b: Float) {
    setPadding(l.dpInt(context), t.dpInt(context), r.dpInt(context), b.dpInt(context))
}

/** Vertical-only padding for stacked text lines; horizontal stays 0. */
fun View.padVertical(context: Context, top: Float, bottom: Float) {
    setPadding(0, top.dpInt(context), 0, bottom.dpInt(context))
}

/**
 * Rounded Kumo surface. When [fullyRounded] is true no fixed radius is set — the caller
 * derives height/2 after layout via [makeCapsule], so chips stay perfectly round
 * regardless of density or font scaling.
 */
fun kumoSurface(
    context: Context,
    fillColor: Int,
    strokeColor: Int? = null,
    strokeWidthDp: Float = KumoDimens.StrokeWidth,
    cornerRadiusDp: Float = KumoDimens.RadiusCard,
    fullyRounded: Boolean = false,
): GradientDrawable = GradientDrawable().apply {
    setColor(fillColor)
    if (!fullyRounded) cornerRadius = cornerRadiusDp.dpPx(context)
    if (strokeColor != null) setStroke(strokeWidthDp.dpInt(context), strokeColor)
}

/**
 * Force a capsule shape whose radius tracks measured height. Call once after the view has
 * been laid out (e.g. via View.post); safe to call again — it recomputes from current height.
 */
fun makeCapsule(view: View) {
    val h = view.height
    if (h > 0) (view.background as? GradientDrawable)?.cornerRadius = h / 2f
}

/** MATCH_PARENT x WRAP_CONTENT LinearLayout child with a top margin in dp. */
fun matchWrapWithTopMargin(context: Context, topMarginDp: Float): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { setMargins(0, topMarginDp.dpInt(context), 0, 0) }
package com.geotagcamera.geotagginglocationonphoto.stamp

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.geotagcamera.geotagginglocationonphoto.ui.theme.MonoDataStyle
import com.geotagcamera.geotagginglocationonphoto.ui.theme.Poppins
import com.geotagcamera.geotagginglocationonphoto.ui.theme.RobotoMono
import com.geotagcamera.geotagginglocationonphoto.ui.theme.StampAnchorStyle

/**
 * One draw path for all three stamp templates, shared by both the live
 * viewfinder overlay (a `Canvas` composable, inside composition, Phase 5)
 * and the final bitmap burn-in ([StampRenderer], a `CanvasDrawScope`
 * wrapping the target `android.graphics.Canvas`, outside composition). Both
 * call the same [draw] against the same [StampSpec] — that identity is what
 * makes "what you frame is what burns in" a guarantee, not a coincidence
 * between two separately-maintained implementations.
 *
 * Card/Bar/Minimal are three branches sharing the same row-building idea:
 * nothing is positioned by a fixed index, every row's presence is read
 * straight off [StampSpec], so the layout closes the gap itself when a
 * field is off — never a hole where the address used to be.
 *
 * No RenderEffect/backdrop blur here (that needs API 31+): a flat
 * semi-transparent scrim is used everywhere, matching the design system's
 * own documented low-end/API-26 fallback path.
 */
object StampPainter {
    private val CardScrim = Color(0xB2141619) // ~rgba(20,22,25,.70), one shade lighter than chrome/base for legibility
    private val CardBorder = Color(0x24FFFFFF) // rgba(255,255,255,.14)
    private val TextPrimary = Color.White
    private val TextSecondary = Color(0xCCFFFFFF) // rgba(255,255,255,.80)
    private val TextMuted = Color(0x8CFFFFFF) // rgba(255,255,255,.55)
    private val ChipBackground = Color(0x1CFFFFFF) // rgba(255,255,255,.11)
    private val BrandDotColor = Color(0xFF56CB98) // accent/verified
    private data class CardLayouts(
        val place: TextLayoutResult?,
        val address: TextLayoutResult?,
        val coords: TextLayoutResult?,
        val date: TextLayoutResult?
    )

    private val AddressStyle = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, color = TextSecondary)
    private val DateTimeStyle = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, color = TextSecondary)
    private val ChipStyle = TextStyle(fontFamily = RobotoMono, fontWeight = FontWeight.Medium, fontSize = 10.5.sp, color = TextPrimary)
    private val MutedChipStyle = ChipStyle.copy(color = TextMuted)
    private val FooterLabelStyle = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = TextPrimary)
    private val CountryChipStyle = TextStyle(fontFamily = RobotoMono, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = Color(0xFF0A0C0E))
    private val BrandTextStyle = TextStyle(fontFamily = RobotoMono, fontWeight = FontWeight.Medium, fontSize = 8.5.sp, color = TextPrimary.copy(alpha = 0.55f), letterSpacing = 1.sp)

    fun draw(scope: DrawScope, spec: StampSpec, textMeasurer: TextMeasurer) {
        when (spec.template) {
            StampTemplate.CARD -> drawCard(scope, spec, textMeasurer)
            StampTemplate.BAR -> drawBar(scope, spec, textMeasurer)
            StampTemplate.MINIMAL -> drawMinimal(scope, spec, textMeasurer)
        }
    }

    // ---- Card: the primary template, a scrim card anchored to one of nine positions ----

    private fun drawCard(scope: DrawScope, spec: StampSpec, tm: TextMeasurer) = with(scope) {
        val boxScale = spec.boxScale.coerceIn(0.5f, 1.6f)
        val textScale = spec.textScale.coerceIn(0.6f, 1.8f)
        val fontFamily = fontFamilyFor(spec.font)
        val overrideColor = spec.textColorArgb?.let { Color(it) }
        val primary = overrideColor ?: TextPrimary
        val secondary = overrideColor?.copy(alpha = 0.8f) ?: TextSecondary

        val addressStyle = AddressStyle.copy(fontFamily = fontFamily, fontSize = AddressStyle.fontSize * textScale, color = secondary)
        val dateTimeStyle = DateTimeStyle.copy(fontFamily = fontFamily, fontSize = DateTimeStyle.fontSize * textScale, color = secondary)
        val chipStyle = ChipStyle.copy(fontFamily = fontFamily, fontSize = ChipStyle.fontSize * textScale, color = primary)
        val mutedChipStyle = chipStyle.copy(color = TextMuted)
        val footerLabelStyle = FooterLabelStyle.copy(fontFamily = fontFamily, fontSize = FooterLabelStyle.fontSize * textScale, color = primary)
        val placeStyle = StampAnchorStyle.copy(fontFamily = fontFamily, fontSize = StampAnchorStyle.fontSize * textScale, color = primary)
        val coordsStyle = MonoDataStyle.copy(fontFamily = fontFamily, fontSize = MonoDataStyle.fontSize * textScale, color = primary)

        val px3 = px(3f) * boxScale; val px1 = px(1f)
        val margin = size.minDimension * 0.035f
        val maxCardWidth = size.width * 0.92f
        // Long values wrap inside a stable card instead of making it grow over
        // most of the photo.
        val baseWidth = if (spec.mapTile != null) size.width * 0.88f else size.width * 0.74f
        val cardWidth = (baseWidth * boxScale).coerceIn(size.width * 0.56f, maxCardWidth)
        val pad = cardWidth * 0.045f
        val gap = cardWidth * 0.03f

        val tileSize = if (spec.mapTile != null) cardWidth * 0.26f else 0f
        val hasTile = spec.mapTile != null

        // Text column width: whatever's left of the card after padding and the
        // (optional) map tile. Every line is constrained to this and wraps or
        // ellipsizes, so a long address can never run past the card or off the
        // photo edge (the device-tested failure this fixes).
        val textColWidth = (cardWidth - pad * 2 - (if (hasTile) tileSize + gap else 0f)).coerceAtLeast(1f)
        val measure: (String, TextStyle, Int, Float) -> TextLayoutResult = { text, style, maxLines, maxW ->
            tm.measure(
                text = text,
                style = style,
                overflow = TextOverflow.Clip,
                maxLines = maxLines,
                constraints = Constraints(maxWidth = maxW.roundToInt().coerceAtLeast(1))
            )
        }

        val countryLayout = spec.countryCode?.let { tm.measure(it, CountryChipStyle, maxLines = 1) }
        val countryReserve = countryLayout?.let { it.size.width + px(11f) } ?: 0f
        val dateTimeText = listOfNotNull(spec.dateTimeText, spec.gmtOffsetText).joinToString(" ")
        fun layoutsAt(scale: Float): CardLayouts {
            val place = spec.placeName?.let { measure(it, placeStyle.copy(fontSize = placeStyle.fontSize * scale), 100, (textColWidth - countryReserve).coerceAtLeast(1f)) }
            val address = spec.addressLine?.let { measure(it, addressStyle.copy(fontSize = addressStyle.fontSize * scale), 8, textColWidth) }
            val coords = spec.coordinatesText?.let { measure(it, coordsStyle.copy(fontSize = coordsStyle.fontSize * scale), 3, textColWidth) }
            val date = dateTimeText.takeIf { it.isNotBlank() }?.let { measure(it, dateTimeStyle.copy(fontSize = dateTimeStyle.fontSize * scale), 4, textColWidth) }
            return CardLayouts(place, address, coords, date)
        }
        val maxCardHeight = size.height * 0.34f
        val chipRowHeight = if (spec.chips.isNotEmpty()) with(chipStyle.fontSize) { toPx() } * 2.6f else 0f
        val footerHeight = if (spec.hasFooterRow) with(footerLabelStyle.fontSize) { toPx() } * 2.4f else 0f
        var layoutScale = 1f
        var layouts = layoutsAt(layoutScale)
        while (layoutScale > 0.18f && cardContentHeight(layouts, px3, tileSize, chipRowHeight, footerHeight, spec) > maxCardHeight - pad * 2) {
            layoutScale -= 0.05f
            layouts = layoutsAt(layoutScale)
        }
        val placeLayout = layouts.place
        val addressLayout = layouts.address
        val coordsLayout = layouts.coords
        val dateTimeLayout = layouts.date

        val textBlockHeight = stackedHeight(listOfNotNull(placeLayout, addressLayout, coordsLayout, dateTimeLayout), px3)
        val rowHeight = maxOf(tileSize, textBlockHeight)

        val cardHeight = (pad * 2 +
            rowHeight +
            (if (spec.chips.isNotEmpty()) gap + chipRowHeight else 0f) +
            (if (spec.hasFooterRow) gap + footerHeight else 0f)).coerceAtMost(maxCardHeight)

        val cardOrigin = anchorOrigin(spec, Size(cardWidth, cardHeight), margin)
        val corner = CornerRadius(cardWidth * 0.045f)

        drawRoundRect(color = CardScrim, topLeft = cardOrigin, size = Size(cardWidth, cardHeight), cornerRadius = corner)
        drawRoundRect(color = CardBorder, topLeft = cardOrigin, size = Size(cardWidth, cardHeight), cornerRadius = corner, style = Stroke(width = px1))

        clipRoundRect(cardOrigin, Size(cardWidth, cardHeight), corner) {
            var x = cardOrigin.x + pad
            val rowTop = cardOrigin.y + pad

            if (hasTile) {
            val tile = spec.mapTile!!
            val tileCorner = CornerRadius(tileSize * 0.12f)
            clipRoundRect(Offset(x, rowTop), Size(tileSize, tileSize), tileCorner) {
                drawImage(tile, dstOffset = IntOffset(x.toInt(), rowTop.toInt()), dstSize = IntSize(tileSize.toInt(), tileSize.toInt()))
            }
            x += tileSize + gap
            }

            var textY = rowTop
            placeLayout?.let { layout ->
            drawText(layout, topLeft = Offset(x, textY))
            countryLayout?.let { cl ->
                val chipPad = px(4f)
                val chipX = x + layout.size.width + px(7f)
                val chipSize = Size(cl.size.width + chipPad * 2, cl.size.height + chipPad * 1.2f)
                drawRoundRect(Color(0xFFE9EBEC), Offset(chipX, textY + px1), chipSize, CornerRadius(px(3f)))
                drawText(cl, topLeft = Offset(chipX + chipPad, textY + px1 + chipPad * 0.6f))
            }
            textY += layout.size.height + px3
            }
            addressLayout?.let { layout -> drawText(layout, topLeft = Offset(x, textY)); textY += layout.size.height + px3 }
            coordsLayout?.let { layout -> drawText(layout, topLeft = Offset(x, textY)); textY += layout.size.height + px3 }
            dateTimeLayout?.let { layout -> drawText(layout, topLeft = Offset(x, textY)) }

            var y = rowTop + rowHeight

            if (spec.chips.isNotEmpty()) {
            y += gap
            var chipX = cardOrigin.x + pad
            spec.chips.forEach { chip ->
                val layout = tm.measure(chip.text, chipStyle, maxLines = 1)
                val chipPad = px(5f)
                val chipSize = Size(layout.size.width + chipPad * 2, layout.size.height + chipPad * 1.2f)
                drawRoundRect(ChipBackground, Offset(chipX, y), chipSize, CornerRadius(px(4f)))
                drawText(layout, topLeft = Offset(chipX + chipPad, y + chipPad * 0.6f))
                chipX += chipSize.width + px(5f)
            }
            y += chipRowHeight
            }

            if (spec.hasFooterRow) {
            y += gap
            drawLine(CardBorder, Offset(cardOrigin.x + pad, y), Offset(cardOrigin.x + cardWidth - pad, y), strokeWidth = px1)
            val centerY = y + footerHeight * 0.5f + gap * 0.2f

            // Left: logo, vertically centred.
            var leftX = cardOrigin.x + pad
            spec.orgLogo?.let { logo ->
                val logoSize = footerHeight * 0.72f
                val top = centerY - logoSize / 2f
                clipRoundRect(Offset(leftX, top), Size(logoSize, logoSize), CornerRadius(logoSize * 0.22f)) {
                    drawImage(logo, dstOffset = IntOffset(leftX.toInt(), top.toInt()), dstSize = IntSize(logoSize.toInt(), logoSize.toInt()))
                }
                leftX += logoSize + gap * 0.6f
            }

            // Right: brand mark (rightmost), then SIGNED to its left — laid out
            // right-to-left so nothing collides, each vertically centred.
            var rightX = cardOrigin.x + cardWidth - pad
            if (spec.showBrandMark) {
                val markSize = px(8f)
                val label = tm.measure("TRACELENS", BrandTextStyle.copy(fontFamily = fontFamily, fontSize = 6.5.sp), maxLines = 1)
                val startX = rightX - (markSize + px(4f) + label.size.width)
                drawRoundRect(primary.copy(alpha = 0.55f), Offset(startX, centerY - markSize / 2f), Size(markSize, markSize), CornerRadius(markSize * 0.33f), style = Stroke(width = px(1.2f)))
                drawCircle(BrandDotColor.copy(alpha = 0.85f), markSize * 0.15f, Offset(startX + markSize / 2f, centerY))
                drawText(label, topLeft = Offset(startX + markSize + px(4f), centerY - label.size.height / 2f))
                rightX = startX - gap
            }
            if (spec.showSignedMark || spec.showEditedMark) {
                val label = tm.measure(if (spec.showSignedMark) "SIGNED" else "EDITED", mutedChipStyle, maxLines = 1)
                val startX = rightX - label.size.width
                drawLine(CardBorder, Offset(startX - gap * 0.6f, centerY - footerHeight * 0.28f), Offset(startX - gap * 0.6f, centerY + footerHeight * 0.28f), strokeWidth = px1)
                drawText(label, topLeft = Offset(startX, centerY - label.size.height / 2f))
                rightX = startX - gap
            }

            // Org label fills the gap between the logo and the right-hand block.
            spec.orgLabel?.let { label ->
                val avail = (rightX - leftX).coerceAtLeast(1f)
                val layout = fitSingleLine(tm, label, footerLabelStyle, avail)
                drawText(layout, topLeft = Offset(leftX, centerY - layout.size.height / 2f))
            }
            }
        }
    }

    // ---- Bar: a bottom gradient scrim, no card, place+address left, coords+date right ----

    private fun drawBar(scope: DrawScope, spec: StampSpec, tm: TextMeasurer) = with(scope) {
        val px3 = px(3f)
        val pad = size.width * 0.045f
        val leftMax = size.width * 0.52f
        val rightMax = size.width * 0.44f
        val placeLayout = spec.placeName?.let { tm.measure(it, StampAnchorStyle.copy(color = TextPrimary), overflow = TextOverflow.Clip, maxLines = 6, constraints = Constraints(maxWidth = leftMax.roundToInt().coerceAtLeast(1))) }
        val addressLayout = spec.addressLine?.let { tm.measure(it, AddressStyle, overflow = TextOverflow.Clip, maxLines = 8, constraints = Constraints(maxWidth = leftMax.roundToInt().coerceAtLeast(1))) }
        val projectLayout = spec.orgLabel?.takeIf { it.isNotBlank() }?.let { fitSingleLine(tm, it, FooterLabelStyle, leftMax) }
        val coordsLayout = spec.coordinatesText?.let { tm.measure(it, MonoDataStyle.copy(color = TextPrimary), overflow = TextOverflow.Clip, maxLines = 3, constraints = Constraints(maxWidth = rightMax.roundToInt().coerceAtLeast(1))) }
        val dateTimeText = listOfNotNull(spec.dateTimeText, spec.gmtOffsetText).joinToString(" ")
        val dateTimeLayout = dateTimeText.takeIf { it.isNotBlank() }?.let { tm.measure(it, DateTimeStyle, overflow = TextOverflow.Clip, maxLines = 4, constraints = Constraints(maxWidth = rightMax.roundToInt().coerceAtLeast(1))) }

        val leftHeight = stackedHeight(listOfNotNull(placeLayout, addressLayout, projectLayout), px3)
        val rightHeight = stackedHeight(listOfNotNull(coordsLayout, dateTimeLayout), px3)
        val barHeight = (maxOf(leftHeight, rightHeight, 1f) + pad * 1.6f).coerceAtMost(size.height * 0.34f)
        if (placeLayout == null && addressLayout == null && coordsLayout == null && dateTimeLayout == null) return@with

        drawRect(
            color = Color.Black.copy(alpha = 0.72f),
            topLeft = Offset(0f, size.height - barHeight),
            size = Size(size.width, barHeight)
        )

        var leftY = size.height - barHeight + pad * 0.8f
        placeLayout?.let { drawText(it, topLeft = Offset(pad, leftY)); leftY += it.size.height + px3 }
        addressLayout?.let { drawText(it, topLeft = Offset(pad, leftY)); leftY += it.size.height + px3 }
        projectLayout?.let { drawText(it, topLeft = Offset(pad, leftY)) }

        var rightY = size.height - barHeight + pad * 0.8f
        coordsLayout?.let { drawText(it, topLeft = Offset(size.width - pad - it.size.width, rightY)); rightY += it.size.height + px3 }
        dateTimeLayout?.let { drawText(it, topLeft = Offset(size.width - pad - it.size.width, rightY)) }
    }

    // ---- Minimal: a small blurred-look pill, coordinates and date/time only ----

    private fun drawMinimal(scope: DrawScope, spec: StampSpec, tm: TextMeasurer) = with(scope) {
        val margin = size.minDimension * 0.035f
        val pad = px(10f)
        val px2 = px(2f)
        val minimalMax = size.width * 0.82f
        val placeLayout = spec.placeName?.let { tm.measure(it, StampAnchorStyle.copy(color = TextPrimary), overflow = TextOverflow.Clip, maxLines = 5, constraints = Constraints(maxWidth = minimalMax.roundToInt().coerceAtLeast(1))) }
        val addressLayout = spec.addressLine?.let { tm.measure(it, AddressStyle, overflow = TextOverflow.Clip, maxLines = 7, constraints = Constraints(maxWidth = minimalMax.roundToInt().coerceAtLeast(1))) }
        val projectLayout = spec.orgLabel?.takeIf { it.isNotBlank() }?.let { fitSingleLine(tm, it, FooterLabelStyle, minimalMax) }
        val coordsLayout = spec.coordinatesText?.let { tm.measure(it, MonoDataStyle.copy(color = TextPrimary), overflow = TextOverflow.Clip, maxLines = 3, constraints = Constraints(maxWidth = minimalMax.roundToInt().coerceAtLeast(1))) }
        val dateTimeText = listOfNotNull(spec.dateTimeText, spec.gmtOffsetText).joinToString(" ")
        val dateTimeLayout = dateTimeText.takeIf { it.isNotBlank() }?.let { tm.measure(it, DateTimeStyle, overflow = TextOverflow.Ellipsis, maxLines = 2, constraints = Constraints(maxWidth = minimalMax.roundToInt().coerceAtLeast(1))) }
        if (placeLayout == null && addressLayout == null && projectLayout == null && coordsLayout == null && dateTimeLayout == null) return@with

        val width = maxOf(placeLayout?.size?.width ?: 0, addressLayout?.size?.width ?: 0, projectLayout?.size?.width ?: 0, coordsLayout?.size?.width ?: 0, dateTimeLayout?.size?.width ?: 0) + pad * 2
        val height = (stackedHeight(listOfNotNull(placeLayout, addressLayout, projectLayout, coordsLayout, dateTimeLayout), px2) + pad * 1.4f).coerceAtMost(size.height * 0.34f)

        val origin = anchorOrigin(spec, Size(width, height), margin)
        drawRoundRect(CardScrim, origin, Size(width, height), CornerRadius(px(9f)))

        var y = origin.y + pad * 0.7f
        placeLayout?.let { drawText(it, topLeft = Offset(origin.x + pad, y)); y += it.size.height + px2 }
        addressLayout?.let { drawText(it, topLeft = Offset(origin.x + pad, y)); y += it.size.height + px2 }
        projectLayout?.let { drawText(it, topLeft = Offset(origin.x + pad, y)); y += it.size.height + px2 }
        coordsLayout?.let { drawText(it, topLeft = Offset(origin.x + pad, y)); y += it.size.height + px2 }
        dateTimeLayout?.let { drawText(it, topLeft = Offset(origin.x + pad, y)) }
    }

    // ---- Shared helpers ----

    private fun fitSingleLine(tm: TextMeasurer, text: String, style: TextStyle, maxWidth: Float): TextLayoutResult {
        var fontSize = style.fontSize
        var layout = tm.measure(
            text = text,
            style = style,
            overflow = TextOverflow.Clip,
            maxLines = 1,
            constraints = Constraints(maxWidth = maxWidth.roundToInt().coerceAtLeast(1))
        )
        while (layout.hasVisualOverflow && fontSize.value > 6f) {
            fontSize = (fontSize.value - 0.5f).sp
            layout = tm.measure(
                text = text,
                style = style.copy(fontSize = fontSize),
                overflow = TextOverflow.Clip,
                maxLines = 1,
                constraints = Constraints(maxWidth = maxWidth.roundToInt().coerceAtLeast(1))
            )
        }
        return layout
    }

    /** Nine-anchor placement: same grid the viewfinder drag and the Settings position picker use. */
    private fun DrawScope.anchorOrigin(spec: StampSpec, contentSize: Size, margin: Float): Offset {
        val x = (size.width * spec.positionXFraction).coerceIn(
            margin,
            (size.width - contentSize.width - margin).coerceAtLeast(margin)
        )
        val y = (size.height * spec.positionYFraction - contentSize.height).coerceIn(
            margin,
            (size.height - contentSize.height - margin).coerceAtLeast(margin)
        )
        return Offset(x, y)
    }

    private fun DrawScope.legacyAnchorOrigin(anchor: StampAnchor, contentSize: Size, margin: Float): Offset {
        val x = when (anchor) {
            StampAnchor.TOP_LEFT, StampAnchor.MID_LEFT, StampAnchor.BOTTOM_LEFT -> margin
            StampAnchor.TOP_CENTER, StampAnchor.MID_CENTER, StampAnchor.BOTTOM_CENTER -> (size.width - contentSize.width) / 2f
            StampAnchor.TOP_RIGHT, StampAnchor.MID_RIGHT, StampAnchor.BOTTOM_RIGHT -> size.width - contentSize.width - margin
        }
        val y = when (anchor) {
            StampAnchor.TOP_LEFT, StampAnchor.TOP_CENTER, StampAnchor.TOP_RIGHT -> margin
            StampAnchor.MID_LEFT, StampAnchor.MID_CENTER, StampAnchor.MID_RIGHT -> (size.height - contentSize.height) / 2f
            StampAnchor.BOTTOM_LEFT, StampAnchor.BOTTOM_CENTER, StampAnchor.BOTTOM_RIGHT -> size.height - contentSize.height - margin
        }
        return Offset(x, y)
    }

    private fun DrawScope.clipRoundRect(topLeft: Offset, size: Size, corner: CornerRadius, block: DrawScope.() -> Unit) {
        val path = Path().apply {
            addRoundRect(RoundRect(Rect(topLeft, size), corner))
        }
        clipPath(path) { block() }
    }

    /** [value] is in dp; converts to px using this DrawScope's own density. */
    private fun DrawScope.px(value: Float): Float = value.dp.toPx()

    private fun fontFamilyFor(font: StampFont): androidx.compose.ui.text.font.FontFamily = when (font) {
        StampFont.DEFAULT -> Poppins
        StampFont.SERIF -> androidx.compose.ui.text.font.FontFamily.Serif
        StampFont.MONOSPACE -> RobotoMono
        StampFont.ROUNDED -> androidx.compose.ui.text.font.FontFamily.SansSerif
    }

    /** Sum of each layout's height plus one [gap] between consecutive items, never after the last. */
    private fun stackedHeight(layouts: List<TextLayoutResult>, gap: Float): Float =
        if (layouts.isEmpty()) 0f else layouts.sumOf { it.size.height } + gap * (layouts.size - 1)

    private fun cardContentHeight(
        layouts: CardLayouts,
        gap: Float,
        tileSize: Float,
        chipHeight: Float,
        footerHeight: Float,
        spec: StampSpec
    ): Float {
        val textHeight = stackedHeight(listOfNotNull(layouts.place, layouts.address, layouts.coords, layouts.date), gap)
        return maxOf(tileSize, textHeight) +
            (if (spec.chips.isNotEmpty()) gap + chipHeight else 0f) +
            (if (spec.hasFooterRow) gap + footerHeight else 0f)
    }
}

/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.habitly.habitly.ui

import androidx.annotation.FontRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.habitly.habitly.R
import androidx.compose.material3.Text as MaterialText

@Composable
fun AppBackButton(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    borderContrast: Float,
    isRoot: Boolean = false,
    icon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    OutlinedIconButton(
        onClick = onBack,
        shapes = IconButtonDefaults.shapes(),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = backgroundColor,
            contentColor = tint
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = borderContrast)
        ),
        modifier = modifier
            .padding(horizontal = 12.dp)
            .size(IconButtonDefaults.smallContainerSize())
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (isRoot) "Close" else "Back",
            modifier = Modifier.size(IconButtonDefaults.smallIconSize)
        )
    }
}

@Immutable
data class RobotoFlexAxes(
    val wght: Float? = null, // Weight: 100..1000 (default 400)
    val wdth: Float? = null, // Width: 25..151 (default 100)
    val opsz: Float? = null, // Optical size: 8..144 (default 14)
    val grad: Float? = null, // Grade: -200..150 (default 0)
    val slnt: Float? = null, // Slant: -10..0 (default 0)
    val xtra: Float? = null, // Counter width: 323..603 (default 468)
    val xopq: Float? = null, // Thick stroke: 27..175 (default 96)
    val yopq: Float? = null, // Thin stroke: 25..135 (default 79)
    val ytlc: Float? = null, // Lowercase height: 416..570 (default 514)
    val ytuc: Float? = null, // Uppercase height: 528..760 (default 712)
    val ytas: Float? = null, // Ascender height: 649..854 (default 750)
    val ytde: Float? = null, // Descender depth: -305..-98 (default -203)
    val ytfi: Float? = null, // Figure height: 560..788 (default 738)
    val rond: Float? = null  // Roundness (Google Sans Flex): 0..100 (default 0)
) {
    fun toFontFamily(@FontRes fontRes: Int = R.font.roboto_flex): FontFamily {
        val settings = buildList {
            wght?.let { add(FontVariation.weight(it.toInt().coerceIn(100, 1000))) }
            wdth?.let { add(FontVariation.width(it.coerceIn(25f, 151f))) }
            opsz?.let { add(FontVariation.Setting("opsz", it.coerceIn(8f, 144f))) }
            grad?.let { add(FontVariation.Setting("GRAD", it.coerceIn(-200f, 150f))) }
            slnt?.let { add(FontVariation.slant(it.coerceIn(-10f, 0f))) }
            xtra?.let { add(FontVariation.Setting("XTRA", it.coerceIn(323f, 603f))) }
            xopq?.let { add(FontVariation.Setting("XOPQ", it.coerceIn(27f, 175f))) }
            yopq?.let { add(FontVariation.Setting("YOPQ", it.coerceIn(25f, 135f))) }
            ytlc?.let { add(FontVariation.Setting("YTLC", it.coerceIn(416f, 570f))) }
            ytuc?.let { add(FontVariation.Setting("YTUC", it.coerceIn(528f, 760f))) }
            ytas?.let { add(FontVariation.Setting("YTAS", it.coerceIn(649f, 854f))) }
            ytde?.let { add(FontVariation.Setting("YTDE", it.coerceIn(-305f, -98f))) }
            ytfi?.let { add(FontVariation.Setting("YTFI", it.coerceIn(560f, 788f))) }
            rond?.let { add(FontVariation.Setting("ROND", it.coerceIn(0f, 100f))) }
        }
        return FontFamily(
            Font(
                resId = fontRes,
                weight = FontWeight(wght?.toInt()?.coerceIn(100, 1000) ?: 400),
                variationSettings = FontVariation.Settings(*settings.toTypedArray())
            )
        )
    }
}

@Composable
fun rememberRobotoFlexFontFamily(
    wght: Float? = null,
    wdth: Float? = null,
    opsz: Float? = null,
    grad: Float? = null,
    slnt: Float? = null,
    xtra: Float? = null,
    xopq: Float? = null,
    yopq: Float? = null,
    ytlc: Float? = null,
    ytuc: Float? = null,
    ytas: Float? = null,
    ytde: Float? = null,
    ytfi: Float? = null,
    rond: Float? = null,
    @FontRes fontRes: Int = R.font.roboto_flex
): FontFamily {
    val axes = RobotoFlexAxes(
        wght = wght,
        wdth = wdth,
        opsz = opsz,
        grad = grad,
        slnt = slnt,
        xtra = xtra,
        xopq = xopq,
        yopq = yopq,
        ytlc = ytlc,
        ytuc = ytuc,
        ytas = ytas,
        ytde = ytde,
        ytfi = ytfi,
        rond = rond
    )
    return remember(axes, fontRes) {
        axes.toFontFamily(fontRes)
    }
}

/**
 * Drop-in Text composable supporting Roboto Flex and all 13 of its variable axes.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
    // Roboto Flex variable axes
    useRobotoFlex: Boolean = false,
    wght: Float? = null,
    wdth: Float? = null,
    opsz: Float? = null,
    grad: Float? = null,
    slnt: Float? = null,
    xtra: Float? = null,
    xopq: Float? = null,
    yopq: Float? = null,
    ytlc: Float? = null,
    ytuc: Float? = null,
    ytas: Float? = null,
    ytde: Float? = null,
    ytfi: Float? = null,
    rond: Float? = null,
    axes: RobotoFlexAxes? = null,
    @FontRes fontRes: Int = R.font.roboto_flex
) {
    val hasAxisOverrides = useRobotoFlex || axes != null ||
        wght != null || wdth != null || opsz != null || grad != null ||
        slnt != null || xtra != null || xopq != null || yopq != null ||
        ytlc != null || ytuc != null || ytas != null || ytde != null ||
        ytfi != null || rond != null

    val resolvedFontFamily = if (fontFamily != null) {
        fontFamily
    } else if (hasAxisOverrides) {
        val resolvedFontSize = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize
        val autoOpsz = if (resolvedFontSize != TextUnit.Unspecified) resolvedFontSize.value else null
        val resolvedWght = wght ?: axes?.wght ?: (fontWeight ?: style.fontWeight)?.weight?.toFloat()
        val effectiveAxes = RobotoFlexAxes(
            wght = resolvedWght,
            wdth = wdth ?: axes?.wdth,
            opsz = opsz ?: axes?.opsz ?: autoOpsz,
            grad = grad ?: axes?.grad,
            slnt = slnt ?: axes?.slnt,
            xtra = xtra ?: axes?.xtra,
            xopq = xopq ?: axes?.xopq,
            yopq = yopq ?: axes?.yopq,
            ytlc = ytlc ?: axes?.ytlc,
            ytuc = ytuc ?: axes?.ytuc,
            ytas = ytas ?: axes?.ytas,
            ytde = ytde ?: axes?.ytde,
            ytfi = ytfi ?: axes?.ytfi,
            rond = rond ?: axes?.rond
        )
        remember(effectiveAxes, fontRes) {
            effectiveAxes.toFontFamily(fontRes)
        }
    } else {
        null
    }

    MaterialText(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = resolvedFontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}
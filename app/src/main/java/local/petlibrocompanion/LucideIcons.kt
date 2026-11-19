package local.petlibrocompanion

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Lucide icons (https://lucide.dev, ISC license), from lucide-static 1.61.0: 2-unit round strokes
// on a 24-unit grid, as in the original SVGs. Tinted wherever they're used.

private val stroke = SolidColor(Color.Black)

val LucideUsers: ImageVector by lazy {
    ImageVector.Builder("LucideUsers", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M16 3.128a4 4 0 0 1 0 7.744"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M22 21v-2a4 4 0 0 0-3-3.87"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M5 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideBug: ImageVector by lazy {
    ImageVector.Builder("LucideBug", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M12 20v-9"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M14 7a4 4 0 0 1 4 4v3a6 6 0 0 1-12 0v-3a4 4 0 0 1 4-4z"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M14.12 3.88 16 2"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M21 21a4 4 0 0 0-3.81-4"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M21 5a4 4 0 0 1-3.55 3.97"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M22 13h-4"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M3 21a4 4 0 0 1 3.81-4"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M3 5a4 4 0 0 0 3.55 3.97"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M6 13H2"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("m8 2 1.88 1.88"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M9 7.13V6a3 3 0 1 1 6 0v1.13"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideTrash2: ImageVector by lazy {
    ImageVector.Builder("LucideTrash2", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M10 11v6"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M14 11v6"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M3 6h18"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideSlidersHorizontal: ImageVector by lazy {
    ImageVector.Builder("LucideSlidersHorizontal", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M10 5H3"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M12 19H3"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M14 3v4"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M16 17v4"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M21 12h-9"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M21 19h-5"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M21 5h-7"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M8 10v4"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M8 12H3"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideBookmark: ImageVector by lazy {
    ImageVector.Builder("LucideBookmark", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M17 3a2 2 0 0 1 2 2v15a1 1 0 0 1-1.496.868l-4.512-2.578a2 2 0 0 0-1.984 0l-4.512 2.578A1 1 0 0 1 5 20V5a2 2 0 0 1 2-2z"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucidePlay: ImageVector by lazy {
    ImageVector.Builder("LucidePlay", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

/** Lucide's play, filled in, for the solid Start button. */
val LucidePlayFilled: ImageVector by lazy {
    ImageVector.Builder("LucidePlayFilled", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z"), fill = stroke, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideCalendarClock: ImageVector by lazy {
    ImageVector.Builder("LucideCalendarClock", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M16 14v2.2l1.6 1"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M16 2v3"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M21 7.338V5a2 2 0 00-2-2H5a2 2 0 00-2 2v14a2 2 0 002 2h2.338"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M3 9h5.859"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M8 2v3"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M10 16a6 6 0 1 0 12 0a6 6 0 1 0 -12 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideBell: ImageVector by lazy {
    ImageVector.Builder("LucideBell", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M10.268 21a2 2 0 0 0 3.464 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucidePalette: ImageVector by lazy {
    ImageVector.Builder("LucidePalette", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8l.3.4a1.75 1.75 0 0 1-1.4 2.8z"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M13 6.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M17 10.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M6 12.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M8 7.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideVibrate: ImageVector by lazy {
    ImageVector.Builder("LucideVibrate", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("m2 8 2 2-2 2 2 2-2 2"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("m22 8-2 2 2 2-2 2 2 2"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M9 5h6a1 1 0 0 1 1 1v12a1 1 0 0 1 -1 1h-6a1 1 0 0 1 -1 -1v-12a1 1 0 0 1 1 -1Z"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideArchive: ImageVector by lazy {
    ImageVector.Builder("LucideArchive", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M3 3h18a1 1 0 0 1 1 1v3a1 1 0 0 1 -1 1h-18a1 1 0 0 1 -1 -1v-3a1 1 0 0 1 1 -1Z"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M4 8v11a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M10 12h4"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideZap: ImageVector by lazy {
    ImageVector.Builder("LucideZap", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M15.914 4a1.5 1.5 0 00-2.474-1.561l-9 9A1.5 1.5 0 005.5 14h4.002a.5.5 0 01.471.666L8.086 20a1.5 1.5 0 002.475 1.56l9-9A1.5 1.5 0 0018.5 10h-3.997a.5.5 0 01-.472-.667z"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideChartColumn: ImageVector by lazy {
    ImageVector.Builder("LucideChartColumn", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M3 3v16a2 2 0 0 0 2 2h16"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M18 17V9"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M13 17V5"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M8 17v-3"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideHash: ImageVector by lazy {
    ImageVector.Builder("LucideHash", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M4 9L20 9"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M4 15L20 15"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M10 3L8 21"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M16 3L14 21"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideTrophy: ImageVector by lazy {
    ImageVector.Builder("LucideTrophy", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M10 14.66V17a1 1 0 0 1-1 1 2 2 0 0 0-2 2v2"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M14 14.66V17a1 1 0 0 0 1 1 2 2 0 0 1 2 2v2"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M17.916 10H19.5A2.5 2.5 0 0 0 22 7.5V5a1 1 0 0 0-1-1h-3"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M4 22h16"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M6 9a6 6 0 0 0 12 0V3a1 1 0 0 0-1-1H7a1 1 0 0 0-1 1z"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M6.084 10H4.5A2.5 2.5 0 0 1 2 7.5V5a1 1 0 0 1 1-1h3"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideAward: ImageVector by lazy {
    ImageVector.Builder("LucideAward", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("m15.477 12.89 1.515 8.526a.5.5 0 0 1-.81.47l-3.58-2.687a1 1 0 0 0-1.197 0l-3.586 2.686a.5.5 0 0 1-.81-.469l1.514-8.526"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M6 8a6 6 0 1 0 12 0a6 6 0 1 0 -12 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}



val LucideKeyRound: ImageVector by lazy {
    ImageVector.Builder("LucideKeyRound", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M16 7.5a.5.5 0 1 0 1 0a.5.5 0 1 0 -1 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideCheck: ImageVector by lazy {
    ImageVector.Builder("LucideCheck", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M20 6 9 17l-5-5"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideX: ImageVector by lazy {
    ImageVector.Builder("LucideX", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M18 6 6 18"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("m6 6 12 12"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideTriangleAlert: ImageVector by lazy {
    ImageVector.Builder("LucideTriangleAlert", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M12 9v4"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M12 17h.01"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideUser: ImageVector by lazy {
    ImageVector.Builder("LucideUser", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M8 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideChevronUp: ImageVector by lazy {
    ImageVector.Builder("LucideChevronUp", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("m18 15-6-6-6 6"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideChevronDown: ImageVector by lazy {
    ImageVector.Builder("LucideChevronDown", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("m6 9 6 6 6-6"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideChevronRight: ImageVector by lazy {
    ImageVector.Builder("LucideChevronRight", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("m9 18 6-6-6-6"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideChevronLeft: ImageVector by lazy {
    ImageVector.Builder("LucideChevronLeft", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("m15 18-6-6 6-6"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideInfo: ImageVector by lazy {
    ImageVector.Builder("LucideInfo", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M12 16v-4"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M12 8h.01"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideSearch: ImageVector by lazy {
    ImageVector.Builder("LucideSearch", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M3 11a8 8 0 1 0 16 0a8 8 0 1 0 -16 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("m21 21-4.3-4.3"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideRefreshCw: ImageVector by lazy {
    ImageVector.Builder("LucideRefreshCw", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M21 3v5h-5"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M8 16H3v5"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideEllipsisVertical: ImageVector by lazy {
    ImageVector.Builder("LucideEllipsisVertical", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M11 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M11 5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M11 19a1 1 0 1 0 2 0a1 1 0 1 0 -2 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideList: ImageVector by lazy {
    ImageVector.Builder("LucideList", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M3 12h.01"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M3 18h.01"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M3 6h.01"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M8 12h13"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M8 18h13"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M8 6h13"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucidePencil: ImageVector by lazy {
    ImageVector.Builder("LucidePencil", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("m15 5 4 4"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucidePlus: ImageVector by lazy {
    ImageVector.Builder("LucidePlus", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M5 12h14"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M12 5v14"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideArrowLeft: ImageVector by lazy {
    ImageVector.Builder("LucideArrowLeft", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("m12 19-7-7 7-7"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M19 12H5"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideSettings: ImageVector by lazy {
    ImageVector.Builder("LucideSettings", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideCheckCheck: ImageVector by lazy {
    ImageVector.Builder("LucideCheckCheck", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M18 6 7 17l-5-5"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("m22 10-7.5 7.5L13 16"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

val LucideCircleHelp: ImageVector by lazy {
    ImageVector.Builder("LucideCircleHelp", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M12 17h.01"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}
val LucideSunMedium: ImageVector by lazy {
    ImageVector.Builder("LucideSunMedium", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M8 12a4 4 0 1 0 8 0a4 4 0 1 0 -8 0"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M12 3v1"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M12 20v1"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M3 12h1"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("M20 12h1"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("m18.364 5.636-.707.707"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("m6.343 17.657-.707.707"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("m5.636 5.636.707.707"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .addPath(addPathNodes("m17.657 17.657.707.707"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}
val LucideMoon: ImageVector by lazy {
    ImageVector.Builder("LucideMoon", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
}

private fun lucide(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach {
            addPath(addPathNodes(it), fill = null, stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }.build()

val LucidePawPrint: ImageVector by lazy {
    lucide(
        "LucidePawPrint",
        "M9 4a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
        "M16 8a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
        "M18 16a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
        "M9 10a5 5 0 0 1 5 5v3.5a3.5 3.5 0 0 1-6.84 1.045Q6.52 17.48 4.46 16.84A3.5 3.5 0 0 1 5.5 10Z",
    )
}

val LucideHouse: ImageVector by lazy {
    lucide(
        "LucideHouse",
        "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8",
        "M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
    )
}

val LucideHistory: ImageVector by lazy {
    lucide(
        "LucideHistory",
        "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8",
        "M3 3v5h5",
        "M12 7v5l4 2",
    )
}

val LucideMinus: ImageVector by lazy { lucide("LucideMinus", "M5 12h14") }

val LucideClock: ImageVector by lazy {
    lucide("LucideClock", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "M12 6v6l4 2")
}

val LucideLogOut: ImageVector by lazy {
    lucide("LucideLogOut", "m16 17 5-5-5-5", "M21 12H9", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4")
}

val LucideRouter: ImageVector by lazy {
    lucide(
        "LucideRouter",
        "M2 16a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2z",
        "M6.01 18H6", "M10.01 18H10", "M15 10v4",
        "M17.84 7.17a4 4 0 0 0-5.66 0", "M20.66 4.34a8 8 0 0 0-11.31 0",
    )
}

val LucideTrash: ImageVector by lazy {
    lucide("LucideTrash", "M3 6h18", "M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6", "M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2")
}

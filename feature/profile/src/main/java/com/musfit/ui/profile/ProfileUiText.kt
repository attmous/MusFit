package com.musfit.ui.profile

import com.musfit.feature.profile.R
import com.musfit.ui.text.UiText
import com.musfit.ui.text.uiText

internal val MEASUREMENT_LABEL_RESOURCES = mapOf(
    "waist" to R.string.profile_measurement_waist,
    "chest" to R.string.profile_measurement_chest,
    "arms" to R.string.profile_measurement_arms,
    "thighs" to R.string.profile_measurement_thighs,
    "hips" to R.string.profile_measurement_hips,
    "body_fat" to R.string.profile_measurement_body_fat,
)

internal fun List<UiText>.joinedWithMiddleDot(): UiText = reduce { accumulated, next ->
    uiText(
        R.string.profile_join_middle_dot,
        UiText.Argument.Nested(accumulated),
        UiText.Argument.Nested(next),
    )
}

internal fun Throwable.messageOr(resourceId: Int): UiText = message?.let(UiText::Verbatim) ?: uiText(resourceId)

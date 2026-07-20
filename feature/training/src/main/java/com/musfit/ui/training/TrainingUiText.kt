package com.musfit.ui.training

import com.musfit.feature.training.R
import com.musfit.ui.text.UiText
import com.musfit.ui.text.uiText

internal fun List<UiText>.joinedWithMiddleDot(): UiText = reduce { accumulated, next ->
    uiText(
        R.string.training_join_middle_dot,
        UiText.Argument.Nested(accumulated),
        UiText.Argument.Nested(next),
    )
}

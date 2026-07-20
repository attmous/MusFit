package com.musfit.ui.text

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/** Context-free UI copy that is resolved only at the presentation boundary. */
sealed interface UiText {
    data class Resource(
        @StringRes val resourceId: Int,
        val arguments: List<Argument> = emptyList(),
    ) : UiText

    data class Plural(
        @PluralsRes val resourceId: Int,
        val quantity: Int,
        val arguments: List<Argument> = emptyList(),
    ) : UiText

    /** User-authored, remote, or calculated text. Fixed application copy must use resources. */
    data class Verbatim(val value: String) : UiText

    sealed interface Argument {
        @JvmInline
        value class Text(val value: String) : Argument

        @JvmInline
        value class Integer(val value: Int) : Argument

        @JvmInline
        value class LongInteger(val value: Long) : Argument

        @JvmInline
        value class Decimal(val value: Double) : Argument

        @JvmInline
        value class Resource(@StringRes val resourceId: Int) : Argument

        @JvmInline
        value class Nested(val value: UiText) : Argument
    }
}

fun uiText(
    @StringRes resourceId: Int,
    vararg arguments: UiText.Argument,
): UiText = UiText.Resource(resourceId = resourceId, arguments = arguments.toList())

fun pluralUiText(
    @PluralsRes resourceId: Int,
    quantity: Int,
    vararg arguments: UiText.Argument,
): UiText = UiText.Plural(resourceId = resourceId, quantity = quantity, arguments = arguments.toList())

@Suppress("SpreadOperator")
fun UiText.resolve(resources: Resources): String = when (this) {
    is UiText.Resource -> resources.getString(resourceId, *arguments.toFormatArguments(resources))
    is UiText.Plural -> resources.getQuantityString(resourceId, quantity, *arguments.toFormatArguments(resources))
    is UiText.Verbatim -> value
}

@Composable
fun UiText.asString(): String {
    LocalConfiguration.current
    return resolve(LocalContext.current.resources)
}

private fun List<UiText.Argument>.toFormatArguments(resources: Resources): Array<Any> = map { argument ->
    when (argument) {
        is UiText.Argument.Text -> argument.value
        is UiText.Argument.Integer -> argument.value
        is UiText.Argument.LongInteger -> argument.value
        is UiText.Argument.Decimal -> argument.value
        is UiText.Argument.Resource -> resources.getString(argument.resourceId)
        is UiText.Argument.Nested -> argument.value.resolve(resources)
    }
}.toTypedArray()

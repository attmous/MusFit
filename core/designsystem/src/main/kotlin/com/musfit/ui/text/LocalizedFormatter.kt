package com.musfit.ui.text

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object LocalizedFormatter {
    fun number(
        value: Double,
        minimumFractionDigits: Int = 0,
        maximumFractionDigits: Int = 2,
        grouping: Boolean = true,
        locale: Locale = Locale.getDefault(),
    ): String = NumberFormat.getNumberInstance(locale).run {
        isGroupingUsed = grouping
        this.minimumFractionDigits = minimumFractionDigits
        this.maximumFractionDigits = maximumFractionDigits
        format(value)
    }

    fun integer(
        value: Long,
        grouping: Boolean = true,
        locale: Locale = Locale.getDefault(),
        minimumIntegerDigits: Int = 1,
    ): String = NumberFormat.getIntegerInstance(locale).run {
        isGroupingUsed = grouping
        this.minimumIntegerDigits = minimumIntegerDigits
        format(value)
    }

    fun date(
        value: LocalDate,
        style: FormatStyle = FormatStyle.MEDIUM,
        locale: Locale = Locale.getDefault(),
    ): String = value.format(DateTimeFormatter.ofLocalizedDate(style).withLocale(locale))

    fun time(
        value: LocalTime,
        style: FormatStyle = FormatStyle.SHORT,
        locale: Locale = Locale.getDefault(),
    ): String = value.format(DateTimeFormatter.ofLocalizedTime(style).withLocale(locale))
}

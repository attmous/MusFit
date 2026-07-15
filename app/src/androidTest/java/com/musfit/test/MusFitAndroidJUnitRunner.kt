package com.musfit.test

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

/** Applies a build-selected class filter before AndroidJUnitRunner discovers tests. */
class MusFitAndroidJUnitRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle) {
        if (arguments.getString(CLASS_ARGUMENT).isNullOrBlank()) {
            configuredClassFilter()?.takeIf(String::isNotBlank)?.let {
                arguments.putString(CLASS_ARGUMENT, it)
            }
        }
        super.onCreate(arguments)
    }

    private fun configuredClassFilter(): String? =
        context.packageManager
            .queryInstrumentation(targetContext.packageName, PackageManager.GET_META_DATA)
            .firstOrNull { it.name == javaClass.name }
            ?.metaData
            ?.getString(CLASS_FILTER_METADATA)

    private companion object {
        const val CLASS_ARGUMENT = "class"
        const val CLASS_FILTER_METADATA = "com.musfit.test.CLASS_FILTER"
    }
}

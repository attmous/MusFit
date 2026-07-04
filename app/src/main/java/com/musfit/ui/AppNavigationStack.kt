package com.musfit.ui

internal class AppNavigationStack(
    entries: List<AppDestination> = listOf(AppDestination.Today),
) {
    private val mutableEntries = entries.ifEmpty { listOf(AppDestination.Today) }.toMutableList()

    val entries: List<AppDestination>
        get() = mutableEntries.toList()

    val current: AppDestination
        get() = mutableEntries.last()

    val canPop: Boolean
        get() = mutableEntries.size > 1

    fun select(destination: AppDestination) {
        if (destination != current) {
            mutableEntries += destination
        }
    }

    fun replace(destination: AppDestination) {
        if (mutableEntries.size >= 2 && mutableEntries[mutableEntries.lastIndex - 1] == destination) {
            mutableEntries.removeAt(mutableEntries.lastIndex)
        } else {
            mutableEntries[mutableEntries.lastIndex] = destination
        }
    }

    fun pop(): AppDestination? {
        if (!canPop) return null
        mutableEntries.removeAt(mutableEntries.lastIndex)
        return current
    }
}

package com.musfit.benchmark

import android.os.Trace
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.TraceMetric
import androidx.benchmark.traceprocessor.TraceProcessor

private const val PSS_TRACE_PREFIX = "musfitTrainingExerciseImagePssKb="
private val totalPssPattern = Regex("""(?m)^\s*TOTAL PSS:\s*([\d,]+)""")
private val legacyTotalPssPattern = Regex("""(?m)^\s*TOTAL\s+([\d,]+)\s+""")

internal fun MacrobenchmarkScope.traceTargetPss() {
    val memInfo = device.executeShellCommand("dumpsys meminfo $TARGET_PACKAGE")
    val pssKb = parseTotalPssKb(memInfo)
    Trace.beginSection("$PSS_TRACE_PREFIX$pssKb")
    Trace.endSection()
}

internal fun parseTotalPssKb(memInfo: String): Long {
    val encoded = totalPssPattern.find(memInfo)?.groupValues?.get(1)
        ?: legacyTotalPssPattern.find(memInfo)?.groupValues?.get(1)
        ?: error("dumpsys meminfo did not report TOTAL PSS for $TARGET_PACKAGE.")
    return encoded.replace(",", "").toLong()
}

@OptIn(ExperimentalMetricApi::class)
internal class TrainingExerciseImagePssMetric : TraceMetric() {
    override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: TraceProcessor.Session,
    ): List<Measurement> {
        check(captureInfo.targetPackageName == TARGET_PACKAGE) {
            "PSS metric expected $TARGET_PACKAGE but traced ${captureInfo.targetPackageName}."
        }
        val row = traceSession.query(
            """
            SELECT CAST(SUBSTR(name, ${PSS_TRACE_PREFIX.length + 1}) AS INTEGER) AS pss_kb
            FROM slice
            WHERE name GLOB '$PSS_TRACE_PREFIX*'
            ORDER BY ts DESC
            LIMIT 1
            """.trimIndent(),
        ).firstOrNull() ?: error("The exercise-image benchmark trace did not contain its PSS snapshot.")
        return listOf(Measurement("trainingExerciseImagePssKb", row.long("pss_kb").toDouble()))
    }
}

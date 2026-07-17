package com.musfit.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class VerifyModuleGraphTask : DefaultTask() {
    @get:Input
    abstract val edges: MapProperty<String, Set<String>>

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val graph = edges.get().toSortedMap()
        val report = buildString {
            graph.forEach { (source, targets) ->
                appendLine("$source -> ${targets.sorted().joinToString()}")
            }
        }
        reportFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(report)
        }
        val violations = ModuleGraphRules.violations(graph)
        check(violations.isEmpty()) {
            "Forbidden MusFit module dependencies:\n${violations.joinToString("\n")}"
        }
    }
}

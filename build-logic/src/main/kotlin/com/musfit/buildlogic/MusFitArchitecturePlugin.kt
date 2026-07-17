package com.musfit.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

class MusFitArchitecturePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        require(this == rootProject) { "musfit.architecture must be applied to the root project" }
        val verify = tasks.register("verifyModuleGraph", VerifyModuleGraphTask::class.java) {
            group = "verification"
            description = "Reports and verifies the acyclic MusFit project dependency graph."
            reportFile.set(layout.buildDirectory.file("reports/architecture/module-graph.txt"))
        }
        gradle.projectsEvaluated {
            val graph = subprojects.associate { subproject ->
                subproject.path to subproject.configurations
                    .flatMap { configuration -> configuration.dependencies.withType(ProjectDependency::class.java) }
                    .map { dependency -> dependency.path }
                    .toSet()
            }
            verify.configure { edges.set(graph) }
        }
    }
}

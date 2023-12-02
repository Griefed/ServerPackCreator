package de.griefed.common.gradle

import com.github.jk1.license.ModuleData
import com.github.jk1.license.ProjectData

/**
 * Fix for https://github.com/jk1/Gradle-License-Report/issues/187<br>
 * [bblonski/SubprojectLicenseFilter.kt](https://gist.github.com/bblonski/49c25882411817a007577ff486ae12fe)
 */
class SubprojectLicenseFilter : com.github.jk1.license.filter.DependencyFilter {
    override fun filter(source: ProjectData): ProjectData {
        val firstLevelDependencies = source.project.subprojects.flatMap { it ->
            it.configurations.filter { it.isCanBeResolved }.flatMap {
                it.resolvedConfiguration.firstLevelModuleDependencies
            }
        }

        val moduleDataSet = source.allDependencies
            .filter { md ->
                firstLevelDependencies
                    .any { dependency ->
                        md.name == dependency.moduleName &&
                                md.group == dependency.moduleGroup &&
                                md.version == dependency.moduleVersion
                    }
            }

        return object : ProjectData(source.project, source.configurations, source.importedModules) {
            override fun getAllDependencies(): MutableSet<ModuleData> {
                return moduleDataSet.toMutableSet()
            }
        }
    }
}
/* Copyright (C) 2025 Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.common.gradle

import com.github.jk1.license.ModuleData
import com.github.jk1.license.ProjectData
import com.github.jk1.license.render.ReportRenderer
import com.github.jk1.license.util.Files
import org.gradle.api.Project
import java.io.File
import java.net.URI

/**
 * Custom made license renderer for https://github.com/jk1/Gradle-License-Report to generate a decent license
 * agreement for use in installers.
 *
 * @author Griefed
 */
class LicenseAgreementRenderer(private val fileName: String = "LICENSE-AGREEMENT"): ReportRenderer {

    private lateinit var project: Project
    private lateinit var output: File
    private lateinit var outputDir: File
    private var counter: Int = 1

    override fun render(data: ProjectData) {
        project = data.project
        outputDir = File(data.project.rootDir,"licenses")
        output = File(data.project.rootDir,"licenses/${fileName}")
        output.writeText("")
        output.appendText("ServerPackCreator license agreement:\n\n")
        if (project.version.toString().matches(".*(dev|alpha|beta).*".toRegex())) {
            output.appendText("By using a dev- / pre-release version of ServerPackCreator, you agree to do so at your own risk.\n")
            output.appendText("You agree to take responsibility for distributing a possibly faulty server pack\n")
            output.appendText("and will not hold Griefed, or any other party responsible for ServerPackCreator,\n")
            output.appendText("accountable should any issues / problems / errors arise from the use of said\n")
            output.appendText("software's dev- / pre-release version.\n")
            output.appendText("You may, however, report issues encountered when using this version on GitHub:\n")
            output.appendText("https://github.com/Griefed/ServerPackCreator/issues\n\n")
        }
        output.appendText("ServerPackCreator ${project.version} license:\n\n")
        output.appendText(File(data.project.rootDir,"LICENSE").readText() + "\n\n")
        output.appendText("Used libraries / dependencies licenses:\n\n")
        output.appendText("-------------------------------------------------------\n\n")
        output.appendText(getDependencyLicenses(data))
    }

    private fun getDependencyLicenses(data: ProjectData): String {
        var text = ""
        for (dependencyData in data.allDependencies) {
            text += "($counter of ${data.allDependencies.size})"
            text += getDependencyLicense(dependencyData)
            text += "\n#######################################\n\n"
            counter++
        }
        return text
    }

    private fun getDependencyLicense(data: ModuleData): String {
        var projectUrlDone = false
        var text = ""
        if (data.group.isNotEmpty()) {
            text = append(text,"Group:   ${data.group}")
        }
        if (data.name.isNotEmpty()) {
            text = append(text,"Name:    ${data.name}")
        }
        if (data.version.isNotEmpty()) {
            text = append(text,"Version: ${data.version}")
        }
        if (data.poms.isEmpty() && data.manifests.isEmpty()) {
            text = append(text,"No license information found\n\n")
        }
        if (data.manifests.isNotEmpty() && data.poms.isNotEmpty()) {
            val manifest = data.manifests.first()
            val pomData = data.poms.first()
            if (!manifest.url.isNullOrBlank() && !pomData.projectUrl.isNullOrBlank() && manifest.url == pomData.projectUrl) {
                text = append(text,"Project URL: ${manifest.url}\\n\\n")
                projectUrlDone = true
            }
        }
        if (data.manifests.isNotEmpty()) {
            val manifest = data.manifests.first()
            if (!manifest.url.isNullOrBlank() && !projectUrlDone) {
                text = append(text,"Manifest Project URL: ${manifest.url}\n\n")
            }
            if (!manifest.license.isNullOrBlank()) {
                if (Files.maybeLicenseUrl(manifest.licenseUrl)) {
                    text = append(text,"Manifest license URL: ${manifest.licenseUrl}\n\n")
                } else if (manifest.hasPackagedLicense) {
                    text = append(text,"Packaged License File: ${manifest.license}\n\n")
                } else {
                    text = append(text,"Manifest License: ${manifest.license} (Not packaged)\n\n")
                }
            }
        }
        if (data.poms.isNotEmpty()) {
            val pomData = data.poms.first()
            if (pomData.projectUrl.isNotEmpty() && !projectUrlDone) {
                text = append(text,"POM Project URL: ${pomData.projectUrl}\n\n")
            }
            if (pomData.licenses.isNotEmpty()) {
                for (license in pomData.licenses) {
                    text = append(text,"POM License: ${license.name}")
                    if (!license.url.isNullOrBlank()) {
                        text = if (Files.maybeLicenseUrl(license.url)) {
                            append(text," - ${license.url}\n\n")
                        } else {
                            append(text,"License: ${license.url}\n\n")
                        }
                    }
                }
            }
        }
        if (data.licenseFiles.isNotEmpty() && data.licenseFiles.first().fileDetails.isNotEmpty()) {
            text = append(text,"Embedded license:\n")
            for (content in data.licenseFiles.first().fileDetails) {
                text = append(text,File(outputDir,content.file).readText() + "\n")
            }
        } else if (data.poms.isNotEmpty() && data.poms.first().licenses.isNotEmpty()) {
            text = append(text,"POM license(s): \n\n")
            for (license in data.poms.first().licenses) {
                text = append(text,"License: ${license.name}")
                text = append(text,"URL: ${license.url}\n\n")
                if (!license.url.isNullOrBlank() && license.url.endsWith(".txt",ignoreCase = true) && Files.maybeLicenseUrl(license.url)) {
                    try {
                        text = append(text, URI(license.url).toURL().readText() + "\n")
                    } catch (_: Exception) {}
                }
            }
        }
        return text
    }

    private fun append(toAppendTo: String, text: String): String {
        return "$toAppendTo\n$text"
    }
}
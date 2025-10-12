/* Copyright (C) 2024  Griefed
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
package de.griefed.serverpackcreator.app.web.scheduling

import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.xml.sax.SAXException
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException

/**
 * Schedules to cover all kinds of aspects of ServerPackCreator.
 *
 * @author Griefed
 */
@Suppress("unused")
@Service
class VersionRefreshSchedule @Autowired constructor(private val versionMeta: VersionMeta) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    @Scheduled(cron = "\${de.griefed.serverpackcreator.spring.schedules.versions.refresh}")
    private fun refreshVersionLister() {
        try {
            versionMeta.update()
        } catch (ex: IOException) {
            log.error("Could not update VersionMeta.", ex)
        } catch (ex: ParserConfigurationException) {
            log.error("Could not update VersionMeta.", ex)
        } catch (ex: SAXException) {
            log.error("Could not update VersionMeta.", ex)
        }
    }
}
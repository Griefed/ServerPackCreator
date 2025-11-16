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
package de.griefed.serverpackcreator.app.web

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.ModelAndView

/**
 * Simple route controller to ensure some destinations let the user arrive where we want them to,
 * otherwise, we redirect. Because our router mode in VueJS is set to hash, we receive a 404 when we
 * try to access, say, /downloads directly. So we need to redirect to /#/downloads instead, for
 * example.
 *
 * @author Griefed
 */
@Suppress("unused")
@RestController
class RouteController {
    /**
     * Redirect /downloads to /#/downloads.
     *
     * @return Redirects requests for /downloads to /#/downloads
     * @author Griefed
     */
    @RequestMapping("/downloads")
    fun redirectDownloads(): ModelAndView {
        return ModelAndView("redirect:/#/downloads")
    }

    /**
     * Redirect /about to /#/about.
     *
     * @return Redirects requests for /about to /#/about
     * @author Griefed
     */
    @RequestMapping("/about")
    fun redirectAbout(): ModelAndView {
        return ModelAndView("redirect:/#/about")
    }
}
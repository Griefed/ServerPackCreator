/* Copyright (C) 2021  Griefed
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
package de.griefed.serverpackcreator.spring.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 * Simple route controller to ensure some destinations let the user arrive where we want them to, otherwise, we redirect.
 * Because our router mode in VueJS is set to hash, we receive a 404 when we try to access, say, /downloads directly.
 * So we need to redirect to /#/downloads instead, for example.
 * @author Griefed
 */
@SuppressWarnings("SpringMVCViewInspection")
@RestController
public class RouteController {

    /**
     * Redirect all unknown paths to our 404-page.
     * @author Griefed
     * @return Redirects the requester to our error page.
     */
    @RequestMapping("/**/{path:[^.]*}")
    public ModelAndView redirectError() {
        return new ModelAndView("redirect:/#/error");
    }

    /**
     * Redirect /downloads to /#/downloads.
     * @author Griefed
     * @return Redirects requests for /downloads to /#/downloads
     */
    @RequestMapping("/downloads")
    public ModelAndView redirectDownloads() {
        return new ModelAndView("redirect:/#/downloads");
    }

    /**
     * Redirect /logs to /#/logs.
     * @author Griefed
     * @return Redirects requests for /logs to /#/logs
     */
    @RequestMapping("/logs")
    public ModelAndView redirectLogs() {
        return new ModelAndView("redirect:/#/logs");
    }

    /**
     * Redirect /about to /#/about.
     * @author Griefed
     * @return Redirects requests for /about to /#/about
     */
    @RequestMapping("/about")
    public ModelAndView redirectAbout() {
        return new ModelAndView("redirect:/#/about");
    }
}

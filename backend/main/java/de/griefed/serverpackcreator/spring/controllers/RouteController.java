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
 *
 * @author Griefed
 */
@SuppressWarnings("SpringMVCViewInspection")
@RestController
public class RouteController {

    /**
     *
     * @author Griefed
     */
    @RequestMapping("/**/{path:[^.]*}")
    public ModelAndView redirectError() {
        return new ModelAndView("redirect:/#/error");
    }

    /**
     *
     * @author Griefed
     */
    @RequestMapping("/downloads")
    public ModelAndView redirectDownloads() {
        return new ModelAndView("redirect:/#/downloads");
    }

    /**
     *
     * @author Griefed
     */
    @RequestMapping("/logs")
    public ModelAndView redirectLogs() {
        return new ModelAndView("redirect:/#/logs");
    }

    /**
     *
     * @author Griefed
     */
    @RequestMapping("/about")
    public ModelAndView redirectAbout() {
        return new ModelAndView("redirect:/#/about");
    }
}

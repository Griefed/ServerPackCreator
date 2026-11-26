/* Copyright (C) 2026 Griefed
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

import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.webmvc.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

/**
 * Answers every request the servlet container forwards to `/error`.
 *
 * A page URL is redirected to the SPA's `/#/error` page. A failed `/api/` request keeps its own status and gets a
 * `ProblemDetail` body instead: the redirect would end in `index.html` with a 200, which a JSON client takes for a
 * successful response.
 *
 * @author Griefed
 */
@Suppress("unused")
@RestController
class ErrorRedirect : ErrorController {

    @RequestMapping(value = ["/error"])
    fun error(request: HttpServletRequest): ResponseEntity<*> {
        val originalUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) as? String ?: ""
        if (originalUri.startsWith(request.contextPath + API_PREFIX)) {
            val status = HttpStatus.resolve(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) as? Int ?: 500)
                ?: HttpStatus.INTERNAL_SERVER_ERROR
            val problem = ProblemDetail.forStatus(status)
            problem.instance = URI.create(originalUri)
            return ResponseEntity.status(status).body(problem)
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(request.contextPath + "/#/error")).build<Any>()
    }

    private companion object {
        const val API_PREFIX = "/api/"
    }
}

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
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/**
 * Pins what [ErrorRedirect] answers when the servlet container forwards a failed request to `/error`.
 *
 * The forward is simulated with the two attributes Tomcat sets on it, because MockMvc never performs the
 * container's error dispatch itself. A page URL is sent to the SPA's error page; an `/api/` URL keeps its
 * status, because the SPA's tables read whatever body comes back, and the error page's HTML (`index.html`,
 * a 200) rendered as one empty row per character.
 */
internal class ErrorRedirectTest {
    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(ErrorRedirect()).build()

    private fun forwardedError(originalUri: String, status: Int) =
        MockMvcRequestBuilders.get("/error")
            .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, originalUri)
            .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, status)

    @Test
    fun aFailedApiRequestKeepsItsServerErrorStatus() {
        mockMvc.perform(forwardedError("/api/v2/events/all", 500))
            .andExpect(MockMvcResultMatchers.status().isInternalServerError)
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(500))
            .andExpect(MockMvcResultMatchers.jsonPath("$.instance").value("/api/v2/events/all"))
    }

    @Test
    fun anUnknownApiRouteIsANotFound() {
        mockMvc.perform(forwardedError("/api/v2/versions", 404))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.header().doesNotExist("Location"))
    }

    @Test
    fun aFailedPageRequestIsSentToTheErrorPage() {
        mockMvc.perform(forwardedError("/no/such/page", 404))
            .andExpect(MockMvcResultMatchers.status().isFound)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/#/error"))
    }
}

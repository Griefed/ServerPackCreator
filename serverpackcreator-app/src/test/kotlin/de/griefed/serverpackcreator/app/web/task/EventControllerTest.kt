package de.griefed.serverpackcreator.app.web.task

import de.griefed.serverpackcreator.app.web.modpack.ModPackStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/**
 * Characterization tests pinning the request-mappings and response-shapes of [EventController]
 * with a mocked event-service, using a standalone MockMvc — no Spring context, no MongoDB.
 */
internal class EventControllerTest {
    private val eventService: EventService = mockk()
    private val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(EventController(eventService))
        .build()

    /**
     * Builds a minimal queue-event for stubbing service-responses.
     */
    private fun queueEvent(modPackId: String, status: ModPackStatus): QueueEvent {
        val event = QueueEvent()
        event.modPackId = modPackId
        event.status = status
        event.message = "Test event"
        return event
    }

    /**
     * Pins that all events are served as a JSON-list.
     */
    @Test
    fun allEventsAreServedAsList() {
        every { eventService.loadAll() } returns mutableListOf(queueEvent("modpack1", ModPackStatus.QUEUED))
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/events/all"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].modPackId").value("modpack1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").value("QUEUED"))
    }

    /**
     * Pins that events are filterable by modpack- and by serverpack-ID.
     */
    @Test
    fun eventsAreServedByModPackAndServerPackId() {
        every { eventService.loadAllByModPackId("modpack1") } returns
                mutableListOf(queueEvent("modpack1", ModPackStatus.CHECKING))
        every { eventService.loadAllByServerPackId("serverpack1") } returns mutableListOf()
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/events/modpack/modpack1"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].modPackId").value("modpack1"))
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/events/serverpack/serverpack1"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isEmpty)
    }

    /**
     * Pins that the status-path-variable is converted to the ModPackStatus-enum for filtering.
     */
    @Test
    fun eventsAreServedByStatus() {
        every { eventService.loadAllByStatus(ModPackStatus.GENERATED) } returns
                mutableListOf(queueEvent("modpack1", ModPackStatus.GENERATED))
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/events/status/GENERATED"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").value("GENERATED"))
    }
}

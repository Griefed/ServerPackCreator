package de.griefed.serverpackcreator.web.task

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.web.WebService
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.support.destination.JmsDestinationAccessor
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.File
import java.util.*

@ExtendWith(SpringExtension::class)
@SpringBootTest
class ArtemisConfigTest @Autowired constructor(private val jmsTemplate: JmsTemplate) {
    private val log = cachedLoggerOf(this.javaClass)
    private val queueUniqueId = "unique_id"
    private val queueTasks = "tasks.background"

    init {
        WebService(
            ApiWrapper.api(
                File(
                    File("").absoluteFile.parent,
                    "serverpackcreator-api/src/jvmTest/resources/serverpackcreator.properties"
                )
            )
        )
        jmsTemplate.receiveTimeout = JmsDestinationAccessor.RECEIVE_TIMEOUT_NO_WAIT
    }

    @AfterEach
    fun emptyQueue() {
        while (jmsTemplate.receive(queueTasks) != null) {
            log.info("Emptying queue")
        }
    }

    @Test
    fun noUniqueID() {
        for (i in 1..5) {
            log.info("Sending message $i")
            jmsTemplate.convertAndSend(queueTasks, "message $i")
        }
        val size: Int? = jmsTemplate.browse(queueTasks) { _, browser -> Collections.list(browser.enumeration).size }
        val message: String = jmsTemplate.receiveAndConvert(queueTasks)!!.toString()
        Assertions.assertEquals(5, size)
        Assertions.assertEquals("message 1", message)
    }


    @Test
    fun sameID() {
        for (i in 1..5) {
            log.info("Sending message $i")
            jmsTemplate.convertAndSend(
                queueTasks,
                "message $i"
            ) { message ->
                message.setStringProperty(queueUniqueId, "1")
                message
            }
        }
        val size: Int? = jmsTemplate.browse(queueTasks) { _, browser -> Collections.list(browser.enumeration).size }
        val message: String = jmsTemplate.receiveAndConvert(queueTasks)!!.toString()
        Assertions.assertEquals(1, size)
        Assertions.assertEquals("message 5", message)
    }

}
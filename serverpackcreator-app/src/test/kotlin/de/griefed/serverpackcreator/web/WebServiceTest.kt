package de.griefed.serverpackcreator.web

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.app.web.WebService
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.io.File

@SpringBootTest(classes = [WebServiceTest::class])
class WebServiceTest internal constructor() {

    init {
        println(File(".").absolutePath)
        WebService(
            ApiWrapper.api(
                File(
                    File("").absoluteFile.parent,
                    "serverpackcreator-api/src/test/resources/serverpackcreator.properties"
                )
            )
        )
    }


    @Test
    fun contextLoads() {

    }
}
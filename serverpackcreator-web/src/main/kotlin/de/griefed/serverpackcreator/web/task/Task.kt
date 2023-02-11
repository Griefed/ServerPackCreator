/* Copyright (C) 2023  Griefed
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
package de.griefed.serverpackcreator.web.task

import java.io.Serializable

/**
 * [How to implement a task queue using Apache Artemis and Spring Boot](https://dev.to/gotson/how-to-implement-a-task-queue-using-apache-artemis-and-spring-boot-2mme)
 *
 * Huge Thank You to [Gauthier](https://github.com/gotson) for writing the above guide on how to implement a JMS.
 * Without it this implementation of Artemis would have either taken way longer or never happened at all. I managed to
 * translate their Kotlin-code to Java and make the necessary changes to fully implement it in ServerPackCreator.
 *
 * @author Griefed
 */
abstract class Task : Serializable {
    abstract fun uniqueId(): String


}
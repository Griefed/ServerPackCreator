/**
 * Artemis JMS configuration. Setup, configure and provide our JMS, so we can send, request and
 * process our queues which contain various types of tasks. We mainly have two types of tasks:
 * {@code scan} and {@code generate}.<br> The scan task is responsible for handling CurseForge
 * requests. If a CurseForge requests comes in, it is checked for validity and submitted to the
 * {@code generate}-queue if it is valid.<br> The {@code generate}-queue is responsible for starting
 * a server pack generation.
 *
 * @author Griefed
 */
package de.griefed.serverpackcreator.spring.task;

# Module serverpackcreator-web

Web-API of ServerPackCreator.

# Package de.griefed.serverpackcreator.app.web

* ServerPackCreators Webservice.<br></br> This package provides the complete access and control of
* ServerPackCreator functionality through the Spring Boot framework REST API. In the base package
* of our REST API we provide the basic configuration to ensure ServerPackCreator, when run as a
* webservice, works the way we want it to.

# Package de.griefed.serverpackcreator.app.web.serverpack

Everything revolving around server packs in the webservice. The model for database interaction,
REST endpoints for listing available server packs, requesting downloads etc.

# Package de.griefed.serverpackcreator.app.web.task

Artemis JMS configuration. Setup, configure and provide our JMS, so we can send, request and process our queues which
contain various types of tasks. We mainly have two types of tasks:
* `scan` and
* `generate`.

The scan task is responsible for handling CurseForge requests. If a CurseForge requests comes in, it is checked for
validity and submitted to the `generate`-queue if it is valid.<br></br> The `generate`-queue is responsible for starting
a server pack generation.

# Package de.griefed.serverpackcreator.app.web.zip

Everything related to creating a server pack from a modpack ZIP-archive.

# Module serverpackcreator-gui

# Package de.serverpackcreator.gui

The Swing-GUI for ServerPackCreator. The GUI itself holds little to no actual ServerPackCreator
logic, but instead focuses on accessing the underlying logic to allow a user to configure and
generate a server pack. This package creates the complete GUI which can then be used by a given
user.
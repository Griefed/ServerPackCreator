# Server Packs

<api-endpoint openapi-path="./../api-docs.yaml" endpoint="/api/v2/serverpacks/{id}" method="GET">
    <request>
        <sample lang="bash">
            curl --location 'http://localhost:8080/api/v2/serverpacks/1'
        </sample>
    </request>
    <response type="200">
        <sample>
        {
            "id": 1,
            "size": 353.0,
            "runConfiguration": {
                "id": 1,
                "minecraftVersion": "1.16.5",
                "modloader": "Forge",
                "modloaderVersion": "36.2.39",
                "startArgs": [
                    {
                        "id": 1,
                        "argument": "-Xms4G"
                    },
                    { ... },
                    {
                        "id": 22,
                        "argument": "-Daikars.new.flags=true"
                    }
                ],
                "clientMods": [
                    {
                        "id": 1,
                        "mod": "3dskinlayers-"
                    },
                    { ... },
                    {
                        "id": 310,
                        "mod": "yisthereautojump-"
                    }
                ],
                "whitelistedMods": [
                    {
                        "id": 1,
                        "mod": "Ping-Wheel-"
                    }
                ]
            },
            "confirmedWorking": 0,
            "sha256": "7b1883b25771c9c5699cee05036e37dc6418613634283fc1f012ff5109cba42b",
            "downloads": 2,
            "dateCreated": 1707166386123
        }
        </sample>
    </response>
</api-endpoint>

<api-endpoint openapi-path="./../api-docs.yaml" endpoint="/api/v2/serverpacks/vote/{id}&{vote}" method="GET">
    <request>
        <sample lang="bash">
            curl --location 'http://localhost:8080/api/v2/serverpacks/vote/1&up'
        </sample>
    </request>
</api-endpoint>

<api-endpoint openapi-path="./../api-docs.yaml" endpoint="/api/v2/serverpacks/download/{modPackId}&{runConfigurationId}" method="GET">
    <request>
        <sample lang="bash">
            curl --location 'http://localhost:8080/api/v2/serverpacks/download/1&2'
        </sample>
    </request>
</api-endpoint>

<api-endpoint openapi-path="./../api-docs.yaml" endpoint="/api/v2/serverpacks/download/{id}" method="GET">
    <request>
        <sample lang="bash">
            curl --location 'http://localhost:8080/api/v2/serverpacks/download/1'
        </sample>
    </request>
</api-endpoint>

<api-endpoint openapi-path="./../api-docs.yaml" endpoint="/api/v2/serverpacks/all" method="GET">
    <request>
        <sample lang="bash">
            curl --location 'http://localhost:8080/api/v2/serverpacks/all'
        </sample>
    </request>
    <response type="200">
        <sample>
        [
            {
                "id": 1,
                "size": 353.0,
                "runConfiguration": {
                    "id": 1,
                    "minecraftVersion": "1.16.5",
                    "modloader": "Forge",
                    "modloaderVersion": "36.2.39",
                    "startArgs": [
                        {
                            "id": 1,
                            "argument": "-Xms4G"
                        },
                        { ... },
                        {
                            "id": 22,
                            "argument": "-Daikars.new.flags=true"
                        }
                    ],
                    "clientMods": [
                        {
                            "id": 1,
                            "mod": "3dskinlayers-"
                        },
                        { ... },
                        {
                            "id": 310,
                            "mod": "yisthereautojump-"
                        }
                    ],
                    "whitelistedMods": [
                        {
                            "id": 1,
                            "mod": "Ping-Wheel-"
                        }
                    ]
                },
                "confirmedWorking": 0,
                "sha256": "7b1883b25771c9c5699cee05036e37dc6418613634283fc1f012ff5109cba42b",
                "downloads": 2,
                "dateCreated": 1707166386123
            }
        ]
        </sample>    
    </response>
</api-endpoint>
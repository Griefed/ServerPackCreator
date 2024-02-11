# Modpacks

<api-endpoint openapi-path="./../api-docs.yaml" endpoint="/api/v2/modpacks/upload" method="POST">
    <request>
        <sample lang="bash">
        curl --location 'http://localhost:8080/api/v2/modpacks/upload' \
        --form 'file=@"/G:/Minecraft/SCP/Releases/SCP 4/Clients/Survive Create Prosper 4 1.16.5-4.7.0.zip"' \
        --form 'minecraftVersion="1.16.5"' \
        --form 'modloader="Forge"' \
        --form 'modloaderVersion="36.2.39"' \
        --form 'startArgs="-Xmx4G -Xms4G"' \
        --form 'clientMods="moda-, modb, modc"' \
        --form 'whiteListMods="modx-, mody, modz"'
        </sample>
    </request>
    <response type="200">
        <sample>
        {
            "message": "File is being stored and will be queued for checks.",
            "success": true,
            "modPackId": 2,
            "runConfigId": 52,
            "serverPackId": null,
            "status": "QUEUED"
        }
        </sample>
    </response>
    <response type="400">
        <sample>
        {
            "message": "Modpack already exists. Not storing. Match found with hash 54c60e2cc5655eb81e007b12fde1030ed1efd74caa2f965008beff298b787931 in Survive Create Prosper 4.zip (1)",
            "success": false,
            "modPackId": 1,
            "runConfigId": 1,
            "serverPackId": null,
            "status": "ERROR"
        }
        </sample>
        <sample>
        {
            "message": "The modpack you uploaded did not pass validation: The ZIP-file you specified only contains one directory: overrides/. ZIP-files for ServerPackCreator must be full modpacks, with all their contents being in the root of the ZIP-file.",
            "success": false,
            "modPackId": null,
            "runConfigId": 1,
            "serverPackId": null,
            "status": "ERROR"
        }
        </sample>
    </response>
</api-endpoint>

<api-endpoint openapi-path="./../api-docs.yaml" endpoint="/api/v2/modpacks/generate" method="POST">
    <request>
        <sample lang="bash">
            curl --location 'http://localhost:8080/api/v2/modpacks/generate' \
            --form 'id="1"' \
            --form 'minecraftVersion="1.12.2"' \
            --form 'modloader="Forge"' \
            --form 'modloaderVersion="14.23.5.2860"' \
            --form 'startArgs="-Xmx4G -Xms4G"' \
            --form 'clientMods="something-, and-more-"' \
            --form 'whiteListMods="modx-, mody, modz"'
        </sample>
    </request>
    <response type="200">
        <sample>
            {
                "message": "Generation of ServerPack, from existing ModPack, with different config, queued.",
                "success": true,
                "modPackId": 1,
                "runConfigId": 53,
                "serverPackId": null,
                "status": "QUEUED"
            }
        </sample>
    </response>
    <response type="400">
        <sample>
        {
            "message": "Modpack not found.",
            "success": false,
            "modPackId": 1,
            "runConfigId": null,
            "serverPackId": null,
            "status": "ERROR"
        }
        </sample>
        <sample>
        {
            "message": "Server Pack already exists for the requested ModPack and RunConfiguration.",
            "success": false,
            "modPackId": 1,
            "runConfigId": 2,
            "serverPackId": 3,
            "status": "ERROR"
        }
        </sample>
    </response>
</api-endpoint>

<api-endpoint openapi-path="./../api-docs.yaml" endpoint="/api/v2/modpacks/{id}" method="GET">
    <request>
        <sample lang="bash">
            curl --location 'http://localhost:8080/api/v2/modpacks/1'
        </sample>
    </request>
    <response type="200">
        <sample>
        {
            "source": "ZIP",
            "name": "Survive Create Prosper 4.zip",
            "id": 1,
            "size": 489.0,
            "status": "GENERATED",
            "serverPacks": [
                {
                    "id": 1,
                    "size": 353,
                    "downloads": 2,
                    "confirmedWorking": 0,
                    "dateCreated": 1707166386123,
                    "fileID": 1707166382296,
                    "sha256": "7b1883b25771c9c5699cee05036e37dc6418613634283fc1f012ff5109cba42b",
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
                    }
                }
            ],
            "sha256": "54c60e2cc5655eb81e007b12fde1030ed1efd74caa2f965008beff298b787931",
            "versionID": "",
            "dateCreated": 1707165869942,
            "projectID": ""
        }
        </sample>
    </response>
</api-endpoint>

<api-endpoint openapi-path="./../api-docs.yaml" endpoint="/api/v2/modpacks/download/{id}" method="GET">
    <request>
        <sample lang="bash">
            curl --location 'http://localhost:8080/api/v2/modpacks/download/1'
        </sample>
    </request>
</api-endpoint>

<api-endpoint openapi-path="./../api-docs.yaml" endpoint="/api/v2/modpacks/all" method="GET">
    <response type="200">
        <sample>
            [
                {
                    "source": "ZIP",
                    "name": "TNP Limitless 6 - LL6 - Custom.zip",
                    "id": 2,
                    "size": 931.0,
                    "status": "GENERATING",
                    "serverPacks": [],
                    "sha256": "b32c0a93a5965a4ad9da08c2b0f66550abe62b2fc3c25c915df13d8bf0adb267",
                    "versionID": "",
                    "dateCreated": 1707588044832,
                    "projectID": ""
                },
                {
                    "source": "ZIP",
                    "name": "Survive Create Prosper 4.zip",
                    "id": 1,
                    "size": 489.0,
                    "status": "GENERATED",
                    "serverPacks": [
                        {
                            "id": 1,
                            "size": 353,
                            "downloads": 2,
                            "confirmedWorking": 0,
                            "dateCreated": 1707166386123,
                            "fileID": 1707166382296,
                            "sha256": "7b1883b25771c9c5699cee05036e37dc6418613634283fc1f012ff5109cba42b",
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
                            }
                        }
                    ],
                    "sha256": "54c60e2cc5655eb81e007b12fde1030ed1efd74caa2f965008beff298b787931",
                    "versionID": "",
                    "dateCreated": 1707165869942,
                    "projectID": ""
                }
            ]
        </sample>
    </response>
</api-endpoint>


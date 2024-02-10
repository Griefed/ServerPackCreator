# Run Configs

<api-endpoint openapi-path="./../api-docs.yaml" endpoint="/api/v2/runconfigs/{id}" method="GET">
    <request>
        <sample lang="bash">
            curl --location 'http://localhost:8080/api/v2/runconfigs/1'
        </sample>
    </request>
    <response type="200">
        <sample>
        {
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
        </sample>
    </response>
</api-endpoint>

<api-endpoint openapi-path="./../api-docs.yaml" endpoint="/api/v2/runconfigs/all" method="GET">
    <request>
        <sample lang="bash">
            curl --location 'http://localhost:8080/api/v2/runconfigs/all'
        </sample>
    </request>
    <response type="200">
        <sample>
        [
            {
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
            {
                "id": 2,
                "minecraftVersion": "1.16.5",
                "modloader": "Forge",
                "modloaderVersion": "36.2.35",
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
            {
                "id": 3,
                "minecraftVersion": "1.16.5",
                "modloader": "Forge",
                "modloaderVersion": "36.2.32",
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
            {
                "id": 52,
                "minecraftVersion": "1.19.2",
                "modloader": "Forge",
                "modloaderVersion": "43.2.21",
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
            {
                "id": 53,
                "minecraftVersion": "1.12.2",
                "modloader": "Forge",
                "modloaderVersion": "14.23.5.2860",
                "startArgs": [
                    {
                        "id": 2,
                        "argument": "-Xmx4G"
                    },
                    {
                        "id": 1,
                        "argument": "-Xms4G"
                    }
                ],
                "clientMods": [
                    {
                        "id": 352,
                        "mod": "something-"
                    },
                    {
                        "id": 353,
                        "mod": "and-more-"
                    }
                ],
                "whitelistedMods": [
                    {
                        "id": 1,
                        "mod": "Ping-Wheel-"
                    }
                ]
            }
        ]
        </sample>
    </response>
</api-endpoint>
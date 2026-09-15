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
                "-Xms4G",
                ...,
                "-Daikars.new.flags=true"
            ],
            "clientMods": [
                "3dskinlayers-",
                ...,
                "yisthereautojump-"
            ],
            "whitelistedMods": [
                "Ping-Wheel-"
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
                    "-Xms4G",
                    ...,
                    "-Daikars.new.flags=true"
                ],
                "clientMods": [
                    "3dskinlayers-",
                    ...,
                    "yisthereautojump-"
                ],
                "whitelistedMods": [
                    "Ping-Wheel-"
                ]
            },
            {
                "id": 2,
                "minecraftVersion": "1.16.5",
                "modloader": "Forge",
                "modloaderVersion": "36.2.35",
                "startArgs": [
                    "-Xms4G",
                    ...,
                    "-Daikars.new.flags=true"
                ],
                "clientMods": [
                    "3dskinlayers-",
                    ...,
                    "yisthereautojump-"
                ],
                "whitelistedMods": [
                    "Ping-Wheel-"
                ]
            },
            {
                "id": 3,
                "minecraftVersion": "1.16.5",
                "modloader": "Forge",
                "modloaderVersion": "36.2.32",
                "startArgs": [
                    "-Xms4G",
                    ...,
                    "-Daikars.new.flags=true"
                ],
                "clientMods": [
                    "3dskinlayers-",
                    ...,
                    "yisthereautojump-"
                ],
                "whitelistedMods": [
                    "Ping-Wheel-"
                ]
            },
            {
                "id": 52,
                "minecraftVersion": "1.19.2",
                "modloader": "Forge",
                "modloaderVersion": "43.2.21",
                "startArgs": [
                    "-Xms4G",
                    ...,
                    "-Daikars.new.flags=true"
                ],
                "clientMods": [
                    "3dskinlayers-",
                    ...,
                    "yisthereautojump-"
                ],
                "whitelistedMods": [
                    "Ping-Wheel-"
                ]
            },
            {
                "id": 53,
                "minecraftVersion": "1.12.2",
                "modloader": "Forge",
                "modloaderVersion": "14.23.5.2860",
                "startArgs": [
                    "-Xmx4G",
                    "-Xms4G"
                ],
                "clientMods": [
                    "something-",
                    "and-more-"
                ],
                "whitelistedMods": [
                    "Ping-Wheel-"
                ]
            }
        ]
        </sample>
    </response>
</api-endpoint>
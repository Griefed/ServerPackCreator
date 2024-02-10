# Events

<api-endpoint openapi-path="./../api-docs.yaml" endpoint="/api/v2/events/status/{status}" method="GET" generate-samples="true">
    <request>
        <sample lang="bash">
            curl --location 'http://localhost:8080/api/v2/events/status/GENERATED'
        </sample>
    </request>
    <response type="200">
        <sample>
        [
            {
                "id": 57,
                "modPackId": 1,
                "serverPackId": null,
                "status": "GENERATED",
                "message": "Generated ServerPack.",
                "timestamp": 1707166386134,
                "errors": []
            },
            {
                "id": 58,
                "modPackId": 1,
                "serverPackId": 1,
                "status": "GENERATED",
                "message": "Submitted task to queue.",
                "timestamp": 1707166387147,
                "errors": []
            },
            {
                "id": 59,
                "modPackId": 1,
                "serverPackId": 1,
                "status": "GENERATED",
                "message": "Syncing ModPack to database.",
                "timestamp": 1707166387156,
                "errors": []
            },
            {
                "id": 60,
                "modPackId": 1,
                "serverPackId": 1,
                "status": "GENERATED",
                "message": "Syncing ServerPack to database.",
                "timestamp": 1707166387167,
                "errors": []
            }
        ]
        </sample>
    </response>
</api-endpoint>

<api-endpoint openapi-path="./../api-docs.yaml" endpoint="/api/v2/events/serverpack/{id}" method="GET">
    <request>
        <sample lang="bash">
            curl --location 'http://localhost:8080/api/v2/events/serverpack/1'
        </sample>
    </request>
    <response type="200">
        <sample>
        [
            {
                "id": 58,
                "modPackId": 1,
                "serverPackId": 1,
                "status": "GENERATED",
                "message": "Submitted task to queue.",
                "timestamp": 1707166387147,
                "errors": []
            },
            {
                "id": 59,
                "modPackId": 1,
                "serverPackId": 1,
                "status": "GENERATED",
                "message": "Syncing ModPack to database.",
                "timestamp": 1707166387156,
                "errors": []
            },
            {
                "id": 60,
                "modPackId": 1,
                "serverPackId": 1,
                "status": "GENERATED",
                "message": "Syncing ServerPack to database.",
                "timestamp": 1707166387167,
                "errors": []
            }
        ]
        </sample>
    </response>
</api-endpoint>

<api-endpoint openapi-path="./../api-docs.yaml" endpoint="/api/v2/events/modpack/{id}" method="GET">
    <request>
        <sample lang="bash">
            curl --location 'http://localhost:8080/api/v2/events/modpack/1'
        </sample>
    </request>
    <response type="200">
        <sample>
        [
            {
                "id": 1,
                "modPackId": 1,
                "serverPackId": null,
                "status": "QUEUED",
                "message": "Submitted task to queue.",
                "timestamp": 1707165869960,
                "errors": []
            },
            {
                "id": 2,
                "modPackId": 1,
                "serverPackId": null,
                "status": "CHECKING",
                "message": "Checking ModPack for errors.",
                "timestamp": 1707165870248,
                "errors": []
            },
            {
                "id": 3,
                "modPackId": 1,
                "serverPackId": null,
                "status": "CHECKED",
                "message": "ModPack checks passed.",
                "timestamp": 1707165875576,
                "errors": []
            },
            { ... },
            {
                "id": 59,
                "modPackId": 1,
                "serverPackId": 1,
                "status": "GENERATED",
                "message": "Syncing ModPack to database.",
                "timestamp": 1707166387156,
                "errors": []
            },
            {
                "id": 60,
                "modPackId": 1,
                "serverPackId": 1,
                "status": "GENERATED",
                "message": "Syncing ServerPack to database.",
                "timestamp": 1707166387167,
                "errors": []
            },
            {
                "id": 107,
                "modPackId": 1,
                "serverPackId": null,
                "status": "QUEUED",
                "message": "Submitted task to queue.",
                "timestamp": 1707588514289,
                "errors": []
            }
        ]
        </sample>
    </response>
</api-endpoint>

<api-endpoint openapi-path="./../api-docs.yaml" endpoint="/api/v2/events/all" method="GET">
    <request>
        <sample lang="bash">
            curl --location 'http://localhost:8080/api/v2/events/all'
        </sample>
    </request>
    <response type="200">
        <sample>
        [
            {
                "id": 107,
                "modPackId": 1,
                "serverPackId": null,
                "status": "QUEUED",
                "message": "Submitted task to queue.",
                "timestamp": 1707588514289,
                "errors": []
            },
            {
                "id": 106,
                "modPackId": 2,
                "serverPackId": null,
                "status": "GENERATING",
                "message": "Generating ServerPack.",
                "timestamp": 1707588064201,
                "errors": []
            },
            { ... },
            {
                "id": 2,
                "modPackId": 1,
                "serverPackId": null,
                "status": "CHECKING",
                "message": "Checking ModPack for errors.",
                "timestamp": 1707165870248,
                "errors": []
            },
            {
                "id": 1,
                "modPackId": 1,
                "serverPackId": null,
                "status": "QUEUED",
                "message": "Submitted task to queue.",
                "timestamp": 1707165869960,
                "errors": []
            }
        ]
        </sample>
    </response>
</api-endpoint>
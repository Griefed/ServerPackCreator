export interface Todo {
  id: number;
  content: string;
}

export interface Meta {
  totalCount: number;
}

export interface ModPack {
  id: number
  projectID: string
  versionID: string
  dateCreated: number
  name: string
  size: number
  status: string
  source: string
  sha256: string
  serverPacks: Array<ServerPack>
}

export interface ServerPack {
  id: number
  size: number
  downloads: number
  confirmedWorking: number
  dateCreated: number
  sha256: string
  runConfiguration: RunConfiguration
}

export interface RunConfiguration {
  id: number
  minecraftVersion: string
  modloader: string
  modloaderVersion: string
  startArgs: string
  clientMods: Array<ClientMod>
  whitelistedMods: Array<WhitelistedMod>
}

export interface ClientMod {
  id: number
  mod: string
}

export interface WhitelistedMod {
  id: number
  mod: string
}

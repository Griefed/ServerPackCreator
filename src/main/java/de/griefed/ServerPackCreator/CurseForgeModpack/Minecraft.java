package de.griefed.ServerPackCreator.CurseForgeModpack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

public class Minecraft {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String version;
    private List<ModLoaders> modLoaders;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<ModLoaders> getModLoaders() {
        return modLoaders;
    }

    public void setModLoaders(List<ModLoaders> modLoaders) {
        this.modLoaders = modLoaders;
    }

    @Override
    public String toString() {
        return String.format("%s,%s",version,modLoaders);
    }
}

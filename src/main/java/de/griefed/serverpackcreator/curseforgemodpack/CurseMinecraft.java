package de.griefed.serverpackcreator.curseforgemodpack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

class CurseMinecraft {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String version;
    private List<CurseModLoaders> modLoaders;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<CurseModLoaders> getModLoaders() {
        return modLoaders;
    }

    public void setModLoaders(List<CurseModLoaders> modLoaders) {
        this.modLoaders = modLoaders;
    }

    @Override
    public String toString() {
        return String.format("%s,%s",version,modLoaders);
    }
}
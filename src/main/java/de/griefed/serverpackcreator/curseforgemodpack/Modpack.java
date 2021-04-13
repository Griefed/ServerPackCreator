package de.griefed.serverpackcreator.curseforgemodpack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

public class Modpack {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private List<Minecraft> minecraft;
    private String name;
    private String version;
    private String author;
    private List<Files> files;

    public List<Minecraft> getMinecraft() {
        return minecraft;
    }

    public void setMinecraft(List<Minecraft> minecraft) {
        this.minecraft = minecraft;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public List<Files> getFiles() {
        return files;
    }

    public void setFiles(List<Files> files) {
        this.files = files;
    }

    @Override
    public String toString() {
        return String.format(
                        "**** Modpack details ****\n" +
                        "Version & Modloader: %s\n" +
                        "Name: %s\n" +
                        "Version: %s\n" +
                        "Author: %s\n" +
                        "Files: %s",
                minecraft, name, version, author, files
        );
    }
}

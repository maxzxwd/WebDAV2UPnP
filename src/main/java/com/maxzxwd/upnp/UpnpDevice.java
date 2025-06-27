package com.maxzxwd.upnp;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Objects;

public final class UpnpDevice {

    @NotNull
    public final String name;

    @NotNull
    public final URI contentDirectoryUri;

    public UpnpDevice(@NotNull String name, @NotNull URI contentDirectoryUri) {
        this.name = name;
        this.contentDirectoryUri = contentDirectoryUri;
    }

    @Override
    @NotNull
    public String toString() {
        return "UpnpDevice{" +
                "name='" + name + '\'' +
                ", contentDirectoryUri=" + contentDirectoryUri +
                '}';
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        var that = (UpnpDevice) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.contentDirectoryUri, that.contentDirectoryUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, contentDirectoryUri);
    }

}
